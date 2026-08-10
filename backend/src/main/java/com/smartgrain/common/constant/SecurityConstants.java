package com.smartgrain.common.constant;

/**
 * 安全相关常量
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /** JWT Header */
    public static final String JWT_HEADER = "Authorization";
    /** JWT 前缀 */
    public static final String JWT_PREFIX = "Bearer ";
    /** Token 类型 - access */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** Token 类型 - refresh */
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    /** Claim: 用户 ID */
    public static final String CLAIM_USER_ID = "uid";
    /** Claim: 用户名 */
    public static final String CLAIM_USERNAME = "username";
    /** Claim: Token 类型 */
    public static final String CLAIM_TOKEN_TYPE = "type";
    /** Claim: 权限码列表 */
    public static final String CLAIM_AUTHORITIES = "auth";

    /** 内部接口路径前缀 */
    public static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/captcha",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/ws/**"
    };
}
