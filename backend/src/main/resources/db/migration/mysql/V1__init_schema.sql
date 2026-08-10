-- =============================================================================
-- Smart Grain Warehouse Platform - MySQL Schema (Phase 1)
-- Target: MySQL 8.x
-- KingbaseES V8 适配脚本在 kingbase/V1__init_schema.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `smart_grain`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smart_grain`;

-- 取消严格模式对零日期的限制
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------------------------------
-- sys_user 用户表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL COMMENT '雪花 ID',
    `username`        VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`        VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    `nickname`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `email`           VARCHAR(128) NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  NULL COMMENT '手机号',
    `avatar`          VARCHAR(255) NULL COMMENT '头像 URL',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    `last_login_at`   DATETIME     NULL COMMENT '最近登录时间',
    `last_login_ip`   VARCHAR(64)  NULL COMMENT '最近登录 IP',
    `remark`          VARCHAR(255) NULL COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删 1=已删',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`      BIGINT       NULL COMMENT '创建人',
    `updated_by`      BIGINT       NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- -----------------------------------------------------------------------------
-- sys_role 角色表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`           BIGINT       NOT NULL COMMENT '雪花 ID',
    `role_code`    VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `role_name`    VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `description`  VARCHAR(255) NULL COMMENT '描述',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    `builtin`      TINYINT      NOT NULL DEFAULT 0 COMMENT '内置角色(不可删): 0=否 1=是',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   BIGINT       NULL,
    `updated_by`   BIGINT       NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- -----------------------------------------------------------------------------
-- sys_permission 权限表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id`             BIGINT       NOT NULL COMMENT '雪花 ID',
    `parent_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限 ID, 0=根节点',
    `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码: module:resource:action',
    `permission_name` VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `permission_type` TINYINT     NOT NULL COMMENT '类型: 1=菜单 2=按钮 3=接口',
    `path`           VARCHAR(255) NULL COMMENT '前端路由路径(菜单用)',
    `component`      VARCHAR(255) NULL COMMENT '前端组件路径(菜单用)',
    `icon`           VARCHAR(64)  NULL COMMENT '菜单图标',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- -----------------------------------------------------------------------------
-- sys_user_role 用户-角色关系
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `role_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关系表';

-- -----------------------------------------------------------------------------
-- sys_role_permission 角色-权限关系
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT,
    `role_id`       BIGINT   NOT NULL,
    `permission_id` BIGINT   NOT NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关系表';

-- -----------------------------------------------------------------------------
-- audit_log 审计日志
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id`  BIGINT       NULL COMMENT '操作人 ID',
    `operator`     VARCHAR(64)  NULL COMMENT '操作人账号',
    `operation`    VARCHAR(64)  NOT NULL COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/...',
    `module`       VARCHAR(64)  NOT NULL COMMENT '模块',
    `target`       VARCHAR(255) NULL COMMENT '操作目标',
    `method`       VARCHAR(8)   NULL COMMENT 'HTTP 方法',
    `request_url`  VARCHAR(500) NULL COMMENT '请求 URL',
    `request_ip`   VARCHAR(64)  NULL COMMENT '请求 IP',
    `request_body` TEXT         NULL COMMENT '请求参数(脱敏后)',
    `result`       TINYINT      NOT NULL DEFAULT 1 COMMENT '结果: 0=失败 1=成功',
    `error_msg`    VARCHAR(1000) NULL COMMENT '错误信息',
    `cost_ms`      INT          NULL COMMENT '耗时(毫秒)',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_module` (`module`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- =============================================================================
-- 初始数据
-- =============================================================================

-- 角色：1=系统管理员 2=库管员 3=审计员
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `builtin`) VALUES
(1, 'ROLE_ADMIN',    '系统管理员', '拥有全部权限',          1),
(2, 'ROLE_KEEPER',   '库管员',     '负责粮仓、入出库业务', 1),
(3, 'ROLE_AUDITOR',  '审计员',     '只读审计权限',          1);

-- 用户：admin / Admin@123456 (BCrypt 加密)
-- BCrypt($2a$10$...) for 'Admin@123456'
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`, `remark`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', 'admin@smartgrain.cn', 1, '内置管理员'),
(2, 'keeper01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '库管员张三', 'keeper@smartgrain.cn', 1, '内置库管员'),
(3, 'auditor01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '审计员李四', 'auditor@smartgrain.cn', 1, '内置审计员');

-- 用户-角色绑定
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3);

-- 权限（Phase 1 仅录入 IAM 与系统配置相关，业务权限在各阶段补录）
-- 菜单层级：根菜单(1) > 子菜单(1) > 按钮/接口(2/3)
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `permission_type`, `path`, `component`, `icon`, `sort_order`) VALUES
-- 一级菜单
(100, 0, 'system',         '系统管理', 1, '/system',     'Layout',           'Setting',   90),
(110, 0, 'monitor',        '监控中心', 1, '/monitor',    'Layout',           'Monitor',   80),
(120, 0, 'dashboard',      '运维大屏', 1, '/dashboard',  'dashboard/Index',  'Odometer',  10),
-- 系统管理子菜单
(1001, 100, 'system:user',       '用户管理', 1, '/system/user',       'system/user/Index',       'User',      1),
(1002, 100, 'system:role',       '角色管理', 1, '/system/role',       'system/role/Index',       'UserFilled',2),
(1003, 100, 'system:permission', '权限管理', 1, '/system/permission', 'system/permission/Index', 'Key',       3),
(1004, 100, 'system:audit',      '审计日志', 1, '/system/audit',      'system/audit/Index',      'Document',  4),
(1005, 100, 'system:config',     '系统配置', 1, '/system/config',     'system/config/Index',     'Tools',     5),
-- 用户管理按钮/接口
(1011, 1001, 'system:user:view',    '用户查询', 3, NULL, NULL, NULL, 1),
(1012, 1001, 'system:user:create',  '用户新增', 2, NULL, NULL, NULL, 2),
(1013, 1001, 'system:user:update',  '用户修改', 2, NULL, NULL, NULL, 3),
(1014, 1001, 'system:user:delete',  '用户删除', 2, NULL, NULL, NULL, 4),
(1015, 1001, 'system:user:reset',   '密码重置', 2, NULL, NULL, NULL, 5),
-- 角色管理按钮/接口
(1021, 1002, 'system:role:view',    '角色查询',   3, NULL, NULL, NULL, 1),
(1022, 1002, 'system:role:create',  '角色新增',   2, NULL, NULL, NULL, 2),
(1023, 1002, 'system:role:update',  '角色修改',   2, NULL, NULL, NULL, 3),
(1024, 1002, 'system:role:delete',  '角色删除',   2, NULL, NULL, NULL, 4),
(1025, 1002, 'system:role:assign',  '权限分配',   2, NULL, NULL, NULL, 5),
-- 权限管理
(1031, 1003, 'system:permission:view', '权限查询', 3, NULL, NULL, NULL, 1),
-- 审计日志
(1041, 1004, 'audit:log:view',          '审计查询', 3, NULL, NULL, NULL, 1),
-- 系统配置
(1051, 1005, 'system:config:view',      '配置查询', 3, NULL, NULL, NULL, 1),
(1052, 1005, 'system:config:update',    '配置修改', 2, NULL, NULL, NULL, 2);

-- 角色-权限绑定
-- 系统管理员：拥有全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `deleted` = 0;

-- 库管员：仅系统配置查询（演示最小权限，业务权限在 Phase 2 补）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 100), (2, 110), (2, 120),
(2, 1005), (2, 1051);

-- 审计员：仅审计日志查询 + 大屏只读
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 120),
(3, 100), (3, 1004), (3, 1041);

-- =============================================================================
-- Phase 1 完成。后续表(粮库/粮仓/批次/入库/出库/库存/IoT/告警/系统配置)在对应 Phase 录入
-- =============================================================================
