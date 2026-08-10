-- =====================================================
-- V4 告警系统：告警记录 + 告警处理流水
-- =====================================================

-- ---------- 1. 告警记录 alert_record ----------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record` (
    `id`             BIGINT       NOT NULL,
    `alert_code`     VARCHAR(64)  NOT NULL COMMENT '告警编号 AL+yyyyMMdd+seq',
    `sensor_id`      BIGINT       NOT NULL COMMENT '触发传感器',
    `sensor_code`    VARCHAR(32)  NOT NULL COMMENT '传感器编码(冗余)',
    `sensor_name`    VARCHAR(64)  NOT NULL COMMENT '传感器名称(冗余)',
    `warehouse_id`   BIGINT                DEFAULT NULL COMMENT '粮仓ID',
    `depot_id`       BIGINT                DEFAULT NULL COMMENT '粮库ID',
    `sensor_type`    VARCHAR(16)  NOT NULL COMMENT 'TEMP/HUMIDITY/GRAIN_TEMP/PEST/GAS',
    `alert_type`     VARCHAR(16)  NOT NULL COMMENT 'HIGH=超上限/LOW=低于下限/OFFLINE=离线',
    `threshold_value` DECIMAL(10,2)        DEFAULT NULL COMMENT '阈值',
    `actual_value`   DECIMAL(10,2) NOT NULL COMMENT '实际值',
    `unit`           VARCHAR(8)            DEFAULT '' COMMENT '单位',
    `severity`       TINYINT      NOT NULL DEFAULT 2 COMMENT '1=低 2=中 3=高',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING=待处理/PROCESSING=处理中/RESOLVED=已解决/IGNORED=已忽略',
    `triggered_at`   DATETIME     NOT NULL COMMENT '触发时间',
    `resolved_at`    DATETIME              DEFAULT NULL COMMENT '解决时间',
    `remark`         VARCHAR(512)          DEFAULT NULL COMMENT '备注',
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`     BIGINT                DEFAULT NULL,
    `updated_by`     BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alert_code` (`alert_code`, `deleted`),
    KEY `idx_alert_sensor` (`sensor_id`),
    KEY `idx_alert_status` (`status`),
    KEY `idx_alert_triggered` (`triggered_at`),
    KEY `idx_alert_depot` (`depot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录';

-- ---------- 2. 告警处理流水 alert_process_log ----------
DROP TABLE IF EXISTS `alert_process_log`;
CREATE TABLE `alert_process_log` (
    `id`          BIGINT       NOT NULL,
    `alert_id`    BIGINT       NOT NULL COMMENT '告警ID',
    `action`      VARCHAR(16)  NOT NULL COMMENT 'ACK=确认/RESOLVE=解决/IGNORE=忽略/REOPEN=重开',
    `from_status` VARCHAR(16)           DEFAULT NULL,
    `to_status`   VARCHAR(16)  NOT NULL,
    `comment`     VARCHAR(512)          DEFAULT NULL COMMENT '处理意见',
    `process_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `process_by`  BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_processlog_alert` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警处理流水';

-- ---------- 权限 ----------
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `permission_type`, `sort_order`, `status`) VALUES
(400, 0,    'alert',                '告警管理',     1, 1, 1),
(401, 400,  'alert:record',         '告警记录',     1, 1, 1),
(402, 401,  'alert:record:view',    '告警查询',     2, 1, 1),
(403, 401,  'alert:record:process', '告警处理',     2, 2, 1),
(404, 401,  'alert:record:delete',  '告警删除',     2, 3, 1),
(410, 400,  'alert:stats',          '告警统计',     1, 2, 1),
(411, 410,  'alert:stats:view',     '统计查询',     2, 1, 1);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `id` BETWEEN 400 AND 411
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission` WHERE `id` IN (400, 401, 402, 410, 411)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- ---------- 初始告警演示数据 ----------
INSERT INTO `alert_record` (`id`, `alert_code`, `sensor_id`, `sensor_code`, `sensor_name`, `warehouse_id`, `depot_id`, `sensor_type`, `alert_type`, `threshold_value`, `actual_value`, `unit`, `severity`, `status`, `triggered_at`, `resolved_at`, `remark`) VALUES
(9001, 'AL20260727001', 5003, 'S-GT-001',   '1号仓-粮温传感器A', 2001, 1001, 'GRAIN_TEMP', 'HIGH', 28.00, 30.20, '℃',  3, 'RESOLVED', DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), '已通风降温'),
(9002, 'AL20260727002', 5004, 'S-PEST-001', '1号仓-虫害监测仪',  2001, 1001, 'PEST',       'HIGH', 5.00,  7.50,  '头/kg', 2, 'PROCESSING', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, '安排熏蒸作业'),
(9003, 'AL20260727003', 5006, 'S-GAS-001',  '3号仓-气体监测仪',  2003, 1001, 'GAS',        'HIGH', 1000.00, 1245.80, 'ppm', 3, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, NULL),
(9004, 'AL20260727004', 5002, 'S-HUM-001',  '1号仓-湿度传感器A', 2001, 1001, 'HUMIDITY',   'HIGH', 75.00, 82.30, '%',  2, 'PENDING', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL),
(9005, 'AL20260726001', 5005, 'S-TEMP-002', '3号仓-温度传感器A', 2003, 1001, 'TEMP',       'LOW',  5.00,  3.20,  '℃',  2, 'IGNORED', DATE_SUB(NOW(), INTERVAL 28 HOUR), DATE_SUB(NOW(), INTERVAL 26 HOUR), '冬季正常波动');

-- 处理流水
INSERT INTO `alert_process_log` (`id`, `alert_id`, `action`, `from_status`, `to_status`, `comment`, `process_by`) VALUES
(1, 9001, 'ACK',     'PENDING',    'PROCESSING', '已确认，安排通风作业', 1),
(2, 9001, 'RESOLVE', 'PROCESSING', 'RESOLVED',   '通风后粮温恢复 26.5℃', 1),
(3, 9002, 'ACK',     'PENDING',    'PROCESSING', '已确认，安排熏蒸',     1),
(4, 9005, 'IGNORE',  'PENDING',    'IGNORED',    '季节性正常波动',       1);
