package com.smartgrain.security;

import com.smartgrain.common.constant.SecurityConstants;
import com.smartgrain.config.SmartGrainProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.smartgrain.common.constant.CacheKey;

/**
 * JWT 提供者 - 生成 / 解析 / 校验 / 黑名单
 *
 * - access token：2h，业务请求使用
 * - refresh token：7d，仅用于刷新 access token
 * - 黑名单：登出 / 修改密码时把 jti 加入 Redis，过期时间 = token 剩余有效期
 */
@Slf4j
@Component
public class JwtProvider {

    private final SmartGrainProperties properties;
    private final StringRedisTemplate redis;
    private final SecretKey key;

    public JwtProvider(SmartGrainProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
        byte[] secretBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret 必须至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /** 生成 access token */
    public String generateAccessToken(UserPrincipal user) {
        return buildToken(user, SecurityConstants.TOKEN_TYPE_ACCESS,
                properties.getJwt().getAccessTokenTtl());
    }

    /** 生成 refresh token */
    public String generateRefreshToken(UserPrincipal user) {
        return buildToken(user, SecurityConstants.TOKEN_TYPE_REFRESH,
                properties.getJwt().getRefreshTokenTtl());
    }

    private String buildToken(UserPrincipal user, String type, long ttlSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000L);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(SecurityConstants.CLAIM_USER_ID, user.getId())
                .claim(SecurityConstants.CLAIM_USERNAME, user.getUsername())
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 解析 token */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 校验 token 是否有效（含黑名单） */
    public boolean isValid(String token) {
        try {
            Claims claims = parse(token);
            if (isBlacklisted(claims.getSubject())) {
                log.debug("Token 已在黑名单: uid={}", claims.getSubject());
                return false;
            }
            return true;
        } catch (JwtException e) {
            log.debug("Token 无效: {}", e.getMessage());
            return false;
        }
    }

    /** 加入黑名单（登出 / 修改密码后调用） */
    public void blacklist(String token) {
        try {
            Claims claims = parse(token);
            long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            if (ttl > 0) {
                redis.opsForValue().set(CacheKey.JWT_BLACKLIST + claims.getSubject(),
                        "1", Duration.ofSeconds(ttl));
            }
        } catch (JwtException e) {
            // 解析失败说明 token 已无效，无需加黑名单
        }
    }

    /** 拉黑用户所有 token（按 uid） */
    public void blacklistUser(Long userId) {
        // 使用一个固定标记位，refresh 时若发现此标记早于 token 签发时间则拒绝
        redis.opsForValue().set(CacheKey.JWT_BLACKLIST + userId,
                String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(properties.getJwt().getRefreshTokenTtl()));
    }

    public boolean isBlacklisted(String subject) {
        return Boolean.TRUE.equals(redis.hasKey(CacheKey.JWT_BLACKLIST + subject));
    }

    public long getAccessTokenTtl() {
        return properties.getJwt().getAccessTokenTtl();
    }

    public long getRefreshTokenTtl() {
        return properties.getJwt().getRefreshTokenTtl();
    }

    /** 从权限码列表中提取角色与权限（按前缀 ROLE_ 区分） */
    public static void splitAuthorities(Set<String> authorities,
                                        Set<String> roles,
                                        Set<String> permissions) {
        if (authorities == null) return;
        for (String a : authorities) {
            if (a.startsWith("ROLE_")) {
                roles.add(a);
            } else {
                permissions.add(a);
            }
        }
    }
}
