package com.smartgrain.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务码定义
 *
 * 编码规则：
 *   0           成功
 *   4xxxx       客户端错误（与 HTTP 状态码语义对齐）
 *   5xxxx       服务端错误
 *   1xxxx       通用业务错误
 *
 *   40001       参数校验失败
 *   40101       未认证 / Token 缺失或失效
 *   40301       无权限
 *   40401       资源不存在
 *   40901       业务冲突（如库存不足）
 *   42901       请求过于频繁（登录限流等）
 *   50000       系统内部错误
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),

    PARAM_INVALID(40001, "参数校验失败"),
    UNAUTHORIZED(40101, "未认证或登录已失效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),
    TOKEN_INVALID(40103, "Token 无效"),
    FORBIDDEN(40301, "无访问权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "业务冲突"),
    STOCK_INSUFFICIENT(40902, "库存不足"),
    DUPLICATE_KEY(40903, "数据已存在"),
    RATE_LIMITED(42901, "请求过于频繁"),
    LOGIN_LOCKED(42902, "登录失败次数过多，账户已锁定"),

    INTERNAL_ERROR(50000, "系统内部错误"),
    DB_ERROR(50001, "数据库异常"),
    CACHE_ERROR(50002, "缓存异常"),
    RPC_ERROR(50003, "远程调用异常"),

    BUSINESS_ERROR(10000, "业务处理失败"),
    USER_NOT_FOUND(10001, "用户不存在"),
    PASSWORD_INCORRECT(10002, "密码错误"),
    USER_DISABLED(10003, "用户已禁用"),
    ROLE_IN_USE(10004, "角色仍在使用，无法删除"),
    OLD_PASSWORD_INCORRECT(10005, "原密码错误");

    private final int code;
    private final String message;
}
