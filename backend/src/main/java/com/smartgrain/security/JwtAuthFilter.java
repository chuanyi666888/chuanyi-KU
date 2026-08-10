package com.smartgrain.security;

import com.smartgrain.common.constant.SecurityConstants;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.common.api.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * 流程：
 *   1. 从 Header 取 Authorization: Bearer xxx
 *   2. 校验签名 + 过期 + 黑名单
 *   3. 用 uid 重新加载用户（保证权限实时性），写入 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtProvider.isValid(token)) {
            try {
                Claims claims = jwtProvider.parse(token);
                String tokenType = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
                if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                    log.debug("非 access token，跳过认证: type={}", tokenType);
                } else {
                    String username = claims.get(SecurityConstants.CLAIM_USERNAME, String.class);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.warn("JWT 认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.JWT_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.JWT_PREFIX)) {
            return header.substring(SecurityConstants.JWT_PREFIX.length()).trim();
        }
        return null;
    }
}
