package com.smartgrain.service.iam;

import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.constant.SecurityConstants;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.audit.entity.AuditLog;
import com.smartgrain.domain.audit.mapper.AuditLogMapper;
import com.smartgrain.domain.iam.dto.ChangePasswordRequest;
import com.smartgrain.domain.iam.dto.LoginRequest;
import com.smartgrain.domain.iam.dto.LoginResponse;
import com.smartgrain.domain.iam.dto.UserInfo;
import com.smartgrain.domain.iam.entity.SysUser;
import com.smartgrain.domain.iam.mapper.SysUserMapper;
import com.smartgrain.security.AuthorityServiceImpl;
import com.smartgrain.security.JwtProvider;
import com.smartgrain.security.LoginRateLimiter;
import com.smartgrain.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final LoginRateLimiter rateLimiter;
    private final SysUserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    private final AuthorityServiceImpl authorityService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername();
        // 限流检查
        rateLimiter.checkLocked(username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            // 更新最近登录信息
            SysUser update = new SysUser();
            update.setId(principal.getId());
            update.setLastLoginAt(LocalDateTime.now());
            String ip = com.smartgrain.common.util.IpUtils.getClientIp(httpRequest);
            update.setLastLoginIp(ip);
            userMapper.updateById(update);

            // 登录成功清零计数
            rateLimiter.recordSuccess(username);

            // 生成 Token
            String accessToken = jwtProvider.generateAccessToken(principal);
            String refreshToken = jwtProvider.generateRefreshToken(principal);

            // 写审计
            writeAuditLog(principal.getId(), username, "LOGIN", "认证", username, ip, 1, null);

            log.info("[LOGIN] 用户 {} 登录成功 IP={}", username, ip);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProvider.getAccessTokenTtl())
                    .userInfo(buildUserInfo(principal))
                    .build();

        } catch (org.springframework.security.core.AuthenticationException e) {
            rateLimiter.recordFail(username);
            writeAuditLog(null, username, "LOGIN", "认证", username,
                    com.smartgrain.common.util.IpUtils.getClientIp(httpRequest), 0, e.getMessage());
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT, "用户名或密码错误");
        }
    }

    @Override
    public void logout(String accessToken) {
        if (StringUtils.hasText(accessToken)) {
            jwtProvider.blacklist(accessToken);
            try {
                var claims = jwtProvider.parse(accessToken);
                String username = claims.get(SecurityConstants.CLAIM_USERNAME, String.class);
                writeAuditLog(Long.valueOf(claims.getSubject()), username, "LOGOUT", "认证", username, null, 1, null);
                log.info("[LOGOUT] 用户 {} 登出", username);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (!jwtProvider.isValid(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        var claims = jwtProvider.parse(refreshToken);
        String tokenType = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
        if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "非 refresh token");
        }
        // 拉黑旧 refresh
        jwtProvider.blacklist(refreshToken);

        String username = claims.get(SecurityConstants.CLAIM_USERNAME, String.class);
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        Set<String> roles = new HashSet<>();
        Set<String> perms = new HashSet<>();
        authorityService.loadAuthorities(user.getId(), roles, perms);

        UserPrincipal principal = UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .roles(roles)
                .permissions(perms)
                .build();

        String newAccess = jwtProvider.generateAccessToken(principal);
        String newRefresh = jwtProvider.generateRefreshToken(principal);

        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenTtl())
                .userInfo(buildUserInfo(principal))
                .build();
    }

    @Override
    public UserInfo currentUserInfo() {
        var principal = com.smartgrain.common.util.SecurityUtils.getCurrentUser();
        return buildUserInfo(principal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        var principal = com.smartgrain.common.util.SecurityUtils.getCurrentUser();
        SysUser user = userMapper.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_INCORRECT);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        // 拉黑该用户所有 token，强制重新登录
        jwtProvider.blacklistUser(user.getId());
        authorityService.evictUserAuthorities(user.getId());

        writeAuditLog(user.getId(), user.getUsername(), "UPDATE", "认证", "修改密码", null, 1, null);
        log.info("[PASSWORD] 用户 {} 修改了密码", user.getUsername());
    }

    private UserInfo buildUserInfo(UserPrincipal principal) {
        return UserInfo.builder()
                .id(principal.getId())
                .username(principal.getUsername())
                .nickname(principal.getNickname())
                .avatar(principal.getAvatar())
                .roles(principal.getRoles() == null ? List.of() :
                        principal.getRoles().stream().sorted().collect(Collectors.toList()))
                .permissions(principal.getPermissions() == null ? List.of() :
                        principal.getPermissions().stream().sorted().collect(Collectors.toList()))
                .build();
    }

    private void writeAuditLog(Long operatorId, String operator, String operation,
                               String module, String target, String ip,
                               int result, String errorMsg) {
        try {
            AuditLog log = new AuditLog();
            log.setOperatorId(operatorId);
            log.setOperator(operator);
            log.setOperation(operation);
            log.setModule(module);
            log.setTarget(target);
            log.setRequestIp(ip);
            log.setResult(result);
            log.setErrorMsg(errorMsg);
            auditLogMapper.insert(log);
        } catch (Exception e) {
            AuthServiceImpl.log.warn("写入审计日志失败: {}", e.getMessage());
        }
    }
}
