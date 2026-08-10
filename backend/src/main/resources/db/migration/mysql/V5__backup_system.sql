-- =============================================================================
-- V5__backup_system.sql
-- Phase 8: 备份恢复系统 - 权限录入
-- =============================================================================
-- 备份恢复菜单（系统管理下子菜单）
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `permission_type`, `path`, `component`, `icon`, `sort_order`) VALUES
(1006, 100, 'system:backup', '备份恢复', 1, '/system/backup', 'system/backup/Index', 'FolderOpened', 6),
-- 备份管理按钮/接口
(1061, 1006, 'system:backup:view',   '备份查询', 3, NULL, NULL, NULL, 1),
(1062, 1006, 'system:backup:manage', '备份管理', 2, NULL, NULL, NULL, 2);

-- 角色权限绑定
-- 系统管理员：拥有全部新权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1006), (1, 1061), (1, 1062);

-- 审计员：备份查询只读权限（便于审计追溯）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 1006), (3, 1061);
