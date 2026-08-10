-- =====================================================
-- V2 粮库业务核心表（粮库/粮仓/批次/入出库单/库存快照）
-- 兼容 MySQL 8.x 与 KingbaseES V8（避免使用 MySQL 私有语法）
-- =====================================================

-- ---------- 1. 粮库 grain_depot ----------
DROP TABLE IF EXISTS `grain_depot`;
CREATE TABLE `grain_depot` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `code`          VARCHAR(32)  NOT NULL COMMENT '粮库编码',
    `name`          VARCHAR(128) NOT NULL COMMENT '粮库名称',
    `address`       VARCHAR(256)          DEFAULT NULL COMMENT '详细地址',
    `province`      VARCHAR(32)           DEFAULT NULL COMMENT '省份',
    `city`          VARCHAR(32)           DEFAULT NULL COMMENT '城市',
    `district`      VARCHAR(32)           DEFAULT NULL COMMENT '区县',
    `longitude`     DECIMAL(10,6)         DEFAULT NULL COMMENT '经度',
    `latitude`      DECIMAL(10,6)         DEFAULT NULL COMMENT '纬度',
    `capacity_tons` DECIMAL(14,3)         DEFAULT 0    COMMENT '设计库容(吨)',
    `manager`       VARCHAR(64)           DEFAULT NULL COMMENT '负责人',
    `phone`         VARCHAR(32)           DEFAULT NULL COMMENT '联系电话',
    `status`        TINYINT      NOT NULL DEFAULT 1    COMMENT '0=停用 1=正常',
    `remark`        VARCHAR(256)          DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`    BIGINT                DEFAULT NULL,
    `updated_by`    BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_depot_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='粮库';

-- ---------- 2. 粮仓 warehouse ----------
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse` (
    `id`            BIGINT       NOT NULL,
    `depot_id`      BIGINT       NOT NULL COMMENT '所属粮库',
    `code`          VARCHAR(32)  NOT NULL COMMENT '粮仓编码',
    `name`          VARCHAR(128) NOT NULL COMMENT '粮仓名称',
    `type`          VARCHAR(16)  NOT NULL DEFAULT 'FLAT' COMMENT 'FLAT=平房仓/SILO=立筒仓/DOME=浅圆仓',
    `capacity_tons` DECIMAL(14,3)         DEFAULT 0    COMMENT '设计容量(吨)',
    `current_tons`  DECIMAL(14,3)         DEFAULT 0    COMMENT '当前库存(吨,冗余)',
    `status`        TINYINT      NOT NULL DEFAULT 1    COMMENT '0=停用 1=空仓 2=在储 3=满仓 4=维修中',
    `remark`        VARCHAR(256)          DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`    BIGINT                DEFAULT NULL,
    `updated_by`    BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wh_code` (`depot_id`, `code`, `deleted`),
    KEY `idx_wh_depot` (`depot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='粮仓';

-- ---------- 3. 粮食批次 grain_batch ----------
DROP TABLE IF EXISTS `grain_batch`;
CREATE TABLE `grain_batch` (
    `id`             BIGINT       NOT NULL,
    `batch_no`       VARCHAR(32)  NOT NULL COMMENT '批次号(唯一)',
    `grain_type`     VARCHAR(16)  NOT NULL COMMENT 'WHEAT=小麦/RICE=稻谷/CORN=玉米/SOYBEAN=大豆',
    `variety`        VARCHAR(64)           DEFAULT NULL COMMENT '品种',
    `grade`          VARCHAR(8)            DEFAULT NULL COMMENT '等级(一/二/三/等外)',
    `origin`         VARCHAR(128)          DEFAULT NULL COMMENT '产地',
    `production_year` SMALLINT             DEFAULT NULL COMMENT '生产年份',
    `moisture`       DECIMAL(5,2)          DEFAULT NULL COMMENT '水分(%)',
    `impurity`       DECIMAL(5,2)          DEFAULT NULL COMMENT '杂质(%)',
    `test_weight`    DECIMAL(6,1)          DEFAULT NULL COMMENT '容重(g/L)',
    `total_in_tons`  DECIMAL(14,3)         DEFAULT 0    COMMENT '累计入库(吨)',
    `total_out_tons` DECIMAL(14,3)         DEFAULT 0    COMMENT '累计出库(吨)',
    `status`         TINYINT      NOT NULL DEFAULT 1    COMMENT '0=已封批 1=在储 2=已出清',
    `remark`         VARCHAR(256)          DEFAULT NULL,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`     BIGINT                DEFAULT NULL,
    `updated_by`     BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`, `deleted`),
    KEY `idx_batch_type` (`grain_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='粮食批次';

-- ---------- 4. 入库单 inbound_order ----------
DROP TABLE IF EXISTS `inbound_order`;
CREATE TABLE `inbound_order` (
    `id`           BIGINT       NOT NULL,
    `order_no`     VARCHAR(32)  NOT NULL COMMENT '入库单号',
    `batch_id`     BIGINT       NOT NULL COMMENT '批次ID',
    `batch_no`     VARCHAR(32)  NOT NULL COMMENT '批次号(冗余)',
    `depot_id`     BIGINT       NOT NULL,
    `warehouse_id` BIGINT       NOT NULL,
    `weight_tons`  DECIMAL(14,3) NOT NULL COMMENT '入库重量(吨)',
    `inbound_type` VARCHAR(16)  NOT NULL DEFAULT 'PURCHASE' COMMENT 'PURCHASE=收购/TRANSFER=调拨/RELOCATE=移库',
    `carrier`      VARCHAR(64)           DEFAULT NULL COMMENT '承运方',
    `vehicle_no`   VARCHAR(32)           DEFAULT NULL COMMENT '车牌号',
    `quality_report` VARCHAR(256)        DEFAULT NULL COMMENT '质检报告编号',
    `operator_id`  BIGINT                DEFAULT NULL COMMENT '操作员',
    `inbound_at`   DATETIME              DEFAULT NULL COMMENT '入库时间',
    `status`       TINYINT      NOT NULL DEFAULT 0    COMMENT '0=待审 1=已审 2=已驳回',
    `audit_remark` VARCHAR(256)          DEFAULT NULL,
    `audited_by`   BIGINT                DEFAULT NULL,
    `audited_at`   DATETIME              DEFAULT NULL,
    `remark`       VARCHAR(256)          DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`   BIGINT                DEFAULT NULL,
    `updated_by`   BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inbound_no` (`order_no`, `deleted`),
    KEY `idx_inbound_batch` (`batch_id`),
    KEY `idx_inbound_wh` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';

-- ---------- 5. 出库单 outbound_order ----------
DROP TABLE IF EXISTS `outbound_order`;
CREATE TABLE `outbound_order` (
    `id`            BIGINT       NOT NULL,
    `order_no`      VARCHAR(32)  NOT NULL,
    `batch_id`      BIGINT       NOT NULL,
    `batch_no`      VARCHAR(32)  NOT NULL,
    `depot_id`      BIGINT       NOT NULL,
    `warehouse_id`  BIGINT       NOT NULL,
    `weight_tons`   DECIMAL(14,3) NOT NULL COMMENT '出库重量(吨)',
    `outbound_type` VARCHAR(16)  NOT NULL DEFAULT 'SALE' COMMENT 'SALE=销售/TRANSFER=调拨/LOSS=损耗/RELOCATE=移库',
    `customer`      VARCHAR(128)          DEFAULT NULL COMMENT '客户/接收方',
    `vehicle_no`    VARCHAR(32)           DEFAULT NULL,
    `operator_id`   BIGINT                DEFAULT NULL,
    `outbound_at`   DATETIME              DEFAULT NULL,
    `status`        TINYINT      NOT NULL DEFAULT 0    COMMENT '0=待审 1=已审 2=已驳回',
    `audit_remark`  VARCHAR(256)          DEFAULT NULL,
    `audited_by`    BIGINT                DEFAULT NULL,
    `audited_at`    DATETIME              DEFAULT NULL,
    `remark`        VARCHAR(256)          DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`    BIGINT                DEFAULT NULL,
    `updated_by`    BIGINT                DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbound_no` (`order_no`, `deleted`),
    KEY `idx_outbound_batch` (`batch_id`),
    KEY `idx_outbound_wh` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

-- ---------- 6. 库存快照 inventory ----------
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
    `id`             BIGINT       NOT NULL,
    `depot_id`       BIGINT       NOT NULL,
    `warehouse_id`   BIGINT       NOT NULL,
    `batch_id`       BIGINT       NOT NULL,
    `grain_type`     VARCHAR(16)  NOT NULL,
    `current_tons`   DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '当前库存(吨)',
    `frozen_tons`    DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '冻结库存(吨,待出库审核)',
    `available_tons` DECIMAL(14,3) NOT NULL DEFAULT 0 COMMENT '可用库存(吨)',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inv_wh_batch` (`warehouse_id`, `batch_id`),
    KEY `idx_inv_depot` (`depot_id`),
    KEY `idx_inv_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存快照';

-- =====================================================
-- 权限种子数据
-- =====================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `permission_code`, `permission_name`, `permission_type`, `sort_order`, `status`) VALUES
(200, 0,    'grain',                    '粮库业务',     1, 1, 1),
(201, 200,  'grain:depot',              '粮库管理',     1, 1, 1),
(202, 201,  'grain:depot:view',         '粮库查询',     2, 1, 1),
(203, 201,  'grain:depot:create',       '粮库新增',     2, 2, 1),
(204, 201,  'grain:depot:update',       '粮库修改',     2, 3, 1),
(205, 201,  'grain:depot:delete',       '粮库删除',     2, 4, 1),

(210, 200,  'grain:warehouse',          '粮仓管理',     1, 2, 1),
(211, 210,  'grain:warehouse:view',     '粮仓查询',     2, 1, 1),
(212, 210,  'grain:warehouse:create',   '粮仓新增',     2, 2, 1),
(213, 210,  'grain:warehouse:update',   '粮仓修改',     2, 3, 1),
(214, 210,  'grain:warehouse:delete',   '粮仓删除',     2, 4, 1),

(220, 200,  'grain:batch',              '批次管理',     1, 3, 1),
(221, 220,  'grain:batch:view',         '批次查询',     2, 1, 1),
(222, 220,  'grain:batch:create',       '批次新增',     2, 2, 1),
(223, 220,  'grain:batch:update',       '批次修改',     2, 3, 1),
(224, 220,  'grain:batch:delete',       '批次删除',     2, 4, 1),

(230, 200,  'grain:inbound',            '入库管理',     1, 4, 1),
(231, 230,  'grain:inbound:view',       '入库查询',     2, 1, 1),
(232, 230,  'grain:inbound:create',     '入库登记',     2, 2, 1),
(233, 230,  'grain:inbound:audit',      '入库审核',     2, 3, 1),

(240, 200,  'grain:outbound',           '出库管理',     1, 5, 1),
(241, 240,  'grain:outbound:view',      '出库查询',     2, 1, 1),
(242, 240,  'grain:outbound:create',    '出库登记',     2, 2, 1),
(243, 240,  'grain:outbound:audit',     '出库审核',     2, 3, 1),

(250, 200,  'grain:inventory',          '库存查询',     1, 6, 1),
(251, 250,  'grain:inventory:view',     '库存查询',     2, 1, 1);

-- 给 ROLE_ADMIN 角色绑定上述权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `id` BETWEEN 200 AND 251
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 给 ROLE_KEEPER 角色绑定查询+登记权限（不含删除/审核）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, `id` FROM `sys_permission`
WHERE `id` IN (200, 201, 202, 210, 211, 220, 221, 230, 231, 232, 240, 241, 242, 250, 251)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- =====================================================
-- 演示业务数据
-- =====================================================
INSERT INTO `grain_depot` (`id`, `code`, `name`, `address`, `province`, `city`, `district`, `capacity_tons`, `manager`, `phone`, `status`, `remark`) VALUES
(1001, 'GD-BJ-001', '中央储备粮北京直属库', '北京市大兴区黄村镇', '北京市', '北京市', '大兴区', 200000.000, '王建国', '010-61234567', 1, '示范粮库'),
(1002, 'GD-HEB-001', '中央储备粮哈尔滨直属库', '哈尔滨市道外区', '黑龙江省', '哈尔滨市', '道外区', 350000.000, '李冬梅', '0451-82345678', 1, NULL);

INSERT INTO `warehouse` (`id`, `depot_id`, `code`, `name`, `type`, `capacity_tons`, `current_tons`, `status`) VALUES
(2001, 1001, 'W-001', '1号平房仓', 'FLAT', 5000.000, 3200.500, 2),
(2002, 1001, 'W-002', '2号平房仓', 'FLAT', 5000.000, 0.000,       1),
(2003, 1001, 'S-001', '1号立筒仓', 'SILO', 8000.000, 6500.000, 2),
(2004, 1002, 'W-001', '1号平房仓', 'FLAT', 6000.000, 4800.000, 2);

INSERT INTO `grain_batch` (`id`, `batch_no`, `grain_type`, `variety`, `grade`, `origin`, `production_year`, `moisture`, `impurity`, `test_weight`, `total_in_tons`, `total_out_tons`, `status`) VALUES
(3001, 'BH-2026-0001', 'WHEAT',  '济麦22',   '二', '山东德州', 2025, 12.50, 1.20, 770.0, 3200.500, 0.000, 1),
(3002, 'BH-2026-0002', 'CORN',   '郑单958',  '一', '吉林长春', 2025, 13.80, 0.90, 720.0, 6500.000, 0.000, 1),
(3003, 'BH-2026-0003', 'RICE',   '稻花香',   '一', '黑龙江五常', 2025, 14.20, 0.50, 530.0, 4800.000, 0.000, 1);

INSERT INTO `inventory` (`id`, `depot_id`, `warehouse_id`, `batch_id`, `grain_type`, `current_tons`, `frozen_tons`, `available_tons`, `version`) VALUES
(4001, 1001, 2001, 3001, 'WHEAT', 3200.500, 0.000, 3200.500, 0),
(4002, 1001, 2003, 3002, 'CORN',  6500.000, 0.000, 6500.000, 0),
(4003, 1002, 2004, 3003, 'RICE',  4800.000, 0.000, 4800.000, 0);
