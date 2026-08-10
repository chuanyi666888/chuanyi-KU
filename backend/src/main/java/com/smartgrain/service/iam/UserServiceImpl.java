package com.smartgrain.service.iam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.domain.iam.dto.PasswordResetRequest;
import com.smartgrain.domain.iam.dto.UserCreateRequest;
import com.smartgrain.domain.iam.dto.UserUpdateRequest;
import com.smartgrain.domain.iam.entity.SysUser;
import com.smartgrain.domain.iam.entity.SysUserRole;
import com.smartgrain.domain.iam.mapper.SysRoleMapper;
import com.smartgrain.domain.iam.mapper.SysUserMapper;
import com.smartgrain.domain.iam.mapper.SysUserRoleMapper;
import com.smartgrain.domain.iam.vo.RoleVO;
import com.smartgrain.domain.iam.vo.UserVO;
import com.smartgrain.security.AuthorityServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityServiceImpl authorityService;

    @Override
    public PageResult<UserVO> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public UserVO getDetail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        // 用户名唯一性校验
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRemark(request.getRemark());
        user.setStatus(1);
        userMapper.insert(user);

        // 绑定角色
        bindRoles(user.getId(), request.getRoleIds());

        log.info("[USER] 新增用户 id={} username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest request) {
        SysUser user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 内置 admin 不允许禁用
        if ("admin".equals(user.getUsername()) && request.getStatus() != null && request.getStatus() == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置管理员不可禁用");
        }

        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);

        // 重新绑定角色
        if (request.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, request.getId()));
            bindRoles(request.getId(), request.getRoleIds());
            // 清除该用户权限缓存
            authorityService.evictUserAuthorities(request.getId());
        }

        log.info("[USER] 更新用户 id={}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置管理员不可删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
        authorityService.evictUserAuthorities(id);
        log.info("[USER] 删除用户 id={} username={}", id, user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetRequest request) {
        SysUser user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        // 拉黑该用户所有 token
        authorityService.evictUserAuthorities(user.getId());
        log.info("[USER] 重置密码 id={} username={}", user.getId(), user.getUsername());
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) return;
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreatedAt(user.getCreatedAt());

        List<RoleVO> roles = roleMapper.selectRolesByUserId(user.getId())
                .stream().map(r -> {
                    RoleVO rv = new RoleVO();
                    rv.setId(r.getId());
                    rv.setRoleCode(r.getRoleCode());
                    rv.setRoleName(r.getRoleName());
                    return rv;
                }).collect(Collectors.toList());
        vo.setRoles(roles);
        return vo;
    }
}
