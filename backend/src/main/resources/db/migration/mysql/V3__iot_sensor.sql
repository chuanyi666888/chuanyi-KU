-- =====================================================
-- V3 IoT 传感器与实时读数
-- =====================================================

-- ---------- 1. 传感器 iot_sensor ----------
DROP TABLE IF EXISTS `iot_sensor`;
CREATE TABLE `iot_sensor` (
    `id`           BIGINT       NOT NULL,
    `code`         VARCHAR(32)  NOT NULL COMMENT '传感器编码',
    `name`         VARCHAR(64)  NOT NULL COMMENT '传感器名称',
    `warehouse_id` BIGINT       NOT NULL COMMENT '所属粮仓',
    `depot_id`     BIGINT       NOT NULL COMMENT '所属粮库',
    `sensor_type`  VARCHAR(16)  NOT NULL COMMENT 'TEMP=温度/HUMIDITY=湿度/GRAIN_TEMP=粮温/PEST=虫害/GAS=气体',
    `unit`         VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '单位: ℃/%/mg/L 等',
    `min_value`    DECIMAL(10,2)         DEFAULT NULL COMMENT '阈值下限',
    `max_value`    DECIMAL(10,2)         DEFAULT NULL COMMENT '阈值上限',
    `last_value`   DECIMAL(10,2)         DEFAULT NULL COMMENT '最近读数(冗余)',
    `last_at`      DATETIME              DEFAULT NULL COMMENT '最近读数时间',
    `status`       TINYINT      NOT NULL DEFAULT 1    COMMENT '0=离线 1=在线',
    `remark`       VARCHAR(256)          DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   BIGINT                DEFAULT NULL,
    `updated_by`   BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensor_code` (`code`, `deleted`),
    KEY `idx_sensor_wh` (`warehouse_id`),
    KEY `idx_sensor_type` (`sensor_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 传感器';

-- ---------- 2. 读数历史 iot_reading ----------
DROP TABLE IF EXISTS `iot_reading`;
CREATE TABLE `iot_reading` (
    `id`          BIGINT       NOT NULL,
    `sensor_id`   BIGINT       NOT NULL,
    `value`       DECIMAL(10,2) NOT NULL,
    `reading_at`  DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_reading_sensor_time` (`sensor_id`, `reading_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT 读数历史';

-- ---------- 权限 ----------
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `permission_type`, `sort_order`, `status`) VALUES
(300, 0,    'iot',                  'IoT 监控',     1, 1, 1),
(301, 300,  'iot:sensor',           '传感器管理',   1, 1, 1),
(302, 301,  'iot:sensor:view',      '传感器查询',   2, 1, 1),
(303, 301,  'iot:sensor:create',    '传感器新增',   2, 2, 1),
(304, 301,  'iot:sensor:update',    '传感器修改',   2, 3, 1),
(305, 301,  'iot:sensor:delete',    '传感器删除',   2, 4, 1),
(310, 300,  'iot:reading',          '实时读数',     1, 2, 1),
(311, 310,  'iot:reading:view',     '读数查询',     2, 1, 1);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `id` BETWEEN 300 AND 311
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission` WHERE `id` IN (300, 301, 302, 310, 311)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- ---------- 演示传感器数据 ----------
INSERT INTO `iot_sensor` (`id`, `code`, `name`, `warehouse_id`, `depot_id`, `sensor_type`, `unit`, `min_value`, `max_value`, `last_value`, `last_at`, `status`) VALUES
(5001, 'S-TEMP-001', '1号仓-温度传感器A', 2001, 1001, 'TEMP',      '℃',  5.00,  35.00, 22.50, NOW(), 1),
(5002, 'S-HUM-001',  '1号仓-湿度传感器A', 2001, 1001, 'HUMIDITY',  '%',  30.00, 75.00, 58.20, NOW(), 1),
(5003, 'S-GT-001',   '1号仓-粮温传感器A', 2001, 1001, 'GRAIN_TEMP','℃',  5.00,  28.00, 18.80, NOW(), 1),
(5004, 'S-PEST-001', '1号仓-虫害监测仪',  2001, 1001, 'PEST',      '头/kg', 0.00, 5.00,  1.00, NOW(), 1),
(5005, 'S-TEMP-002', '3号仓-温度传感器A', 2003, 1001, 'TEMP',      '℃',  5.00,  35.00, 24.10, NOW(), 1),
(5006, 'S-GAS-001',  '3号仓-气体监测仪',  2003, 1001, 'GAS',       'ppm', 0.00, 1000.00, 380.50, NOW(), 1),
(5007, 'S-TEMP-003', '4号仓-温度传感器A', 2004, 1002, 'TEMP',      '℃',  5.00,  35.00, 21.80, NOW(), 1),
(5008, 'S-HUM-002',  '4号仓-湿度传感器A', 2004, 1002, 'HUMIDITY',  '%',  30.00, 75.00, 55.60, NOW(), 1);
