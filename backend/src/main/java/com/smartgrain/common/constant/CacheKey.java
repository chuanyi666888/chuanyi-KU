package com.smartgrain.common.constant;

/**
 * 缓存 Key 规范
 *
 * 命名规则：{业务域}:{对象}:{子维度}:{id}
 * 示例：
 *   iam:user:perms:1001
 *   iam:user:info:1001
 *   asset:warehouse:list
 *   inventory:stock:1001
 */
public final class CacheKey {

    private CacheKey() {}

    /** 用户权限 */
    public static final String USER_PERMS = "iam:user:perms:";
    /** 用户角色 */
    public static final String USER_ROLES = "iam:user:roles:";
    /** 用户信息 */
    public static final String USER_INFO = "iam:user:info:";

    /** 粮仓列表缓存 */
    public static final String WAREHOUSE_LIST = "asset:warehouse:list";
    /** 粮仓详情 */
    public static final String WAREHOUSE_DETAIL = "asset:warehouse:detail:";

    /** 库存缓存 */
    public static final String STOCK = "inventory:stock:";

    /** Dashboard 汇总 */
    public static final String DASHBOARD_SUMMARY = "dashboard:summary";

    /** 系统配置 */
    public static final String SYSTEM_CONFIG = "system:config:";

    /** 默认过期时间（秒） */
    public static final int TTL_SHORT = 60;          // 1 分钟
    public static final int TTL_MEDIUM = 300;        // 5 分钟
    public static final int TTL_LONG = 1800;         // 30 分钟

    /** JWT 黑名单 */
    public static final String JWT_BLACKLIST = "jwt:blacklist:";

    /** 登录失败计数 */
    public static final String LOGIN_FAIL_COUNT = "login:fail:count:";

    /** 登录锁定 */
    public static final String LOGIN_LOCK = "login:lock:";
}
