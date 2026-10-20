-- ============================================================
-- 既有数据库升级脚本: 增加部门/岗位/用户组织字段
-- 执行: mysql -uroot -p flowable_demo < upgrade-org.sql
--
-- 脚本可重复执行; 已存在的表/字段/索引会被跳过。
-- 升级完成后重启后端, DataInitializer 会幂等补齐示例组织数据。
-- ============================================================

USE `flowable_demo`;

-- ------------------------------------------------------------
-- 1. 新建部门表 / 岗位表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_department`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `dept_code`   VARCHAR(64)  NOT NULL COMMENT '部门编码',
    `dept_name`   VARCHAR(128) NOT NULL COMMENT '部门名称',
    `parent_id`   BIGINT                DEFAULT NULL COMMENT '上级部门ID, NULL 表示根部门',
    `leader_id`   BIGINT                DEFAULT NULL COMMENT '部门负责人用户ID',
    `sort`        INT                   DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT               DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `deleted`     TINYINT               DEFAULT 0 COMMENT '逻辑删除: 1删除 0正常',
    `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_leader_id` (`leader_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

CREATE TABLE IF NOT EXISTS `sys_position`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `position_code`  VARCHAR(64)  NOT NULL COMMENT '岗位编码',
    `position_name`  VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `position_level` INT                   DEFAULT 99 COMMENT '岗位层级, 数字越小级别越高',
    `sort`           INT                   DEFAULT 0 COMMENT '排序号',
    `status`         TINYINT               DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `deleted`        TINYINT               DEFAULT 0 COMMENT '逻辑删除: 1删除 0正常',
    `create_time`    DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_code` (`position_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='岗位表';

-- ------------------------------------------------------------
-- 2. 条件添加字段 / 索引
--    MySQL 8 不支持 ADD COLUMN IF NOT EXISTS, 这里用存储过程实现
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS `flowable_add_column_if_not_exists`;
DROP PROCEDURE IF EXISTS `flowable_add_index_if_not_exists`;

DELIMITER $$

CREATE PROCEDURE `flowable_add_column_if_not_exists`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_table_name
                     AND COLUMN_NAME = p_column_name) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ',
                          p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE `flowable_add_index_if_not_exists`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_table_name
                     AND INDEX_NAME = p_index_name) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD ', p_index_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- sys_user: 所属部门 / 岗位
CALL flowable_add_column_if_not_exists('sys_user', 'dept_id',
    'BIGINT NULL COMMENT ''所属部门ID''');
CALL flowable_add_column_if_not_exists('sys_user', 'position_id',
    'BIGINT NULL COMMENT ''岗位ID''');
CALL flowable_add_index_if_not_exists('sys_user', 'idx_dept_id',
    'INDEX `idx_dept_id` (`dept_id`)');
CALL flowable_add_index_if_not_exists('sys_user', 'idx_position_id',
    'INDEX `idx_position_id` (`position_id`)');

-- leave_request: 申请人与审批人快照, 便于审计与历史追溯
CALL flowable_add_column_if_not_exists('leave_request', 'dept_id',
    'BIGINT NULL COMMENT ''申请人部门ID(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'dept_name',
    'VARCHAR(128) NULL COMMENT ''申请人部门名称(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'position_id',
    'BIGINT NULL COMMENT ''申请人岗位ID(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'position_name',
    'VARCHAR(128) NULL COMMENT ''申请人岗位名称(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'dept_leader_id',
    'BIGINT NULL COMMENT ''部门领导用户ID(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'dept_leader_name',
    'VARCHAR(64) NULL COMMENT ''部门领导姓名(快照)''');
CALL flowable_add_column_if_not_exists('leave_request', 'upper_leader_id',
    'BIGINT NULL COMMENT ''上级领导用户ID(快照, >3天时)''');
CALL flowable_add_column_if_not_exists('leave_request', 'upper_leader_name',
    'VARCHAR(64) NULL COMMENT ''上级领导姓名(快照, >3天时)''');
CALL flowable_add_index_if_not_exists('leave_request', 'idx_leave_dept',
    'INDEX `idx_leave_dept` (`dept_id`)');
CALL flowable_add_index_if_not_exists('leave_request', 'idx_leave_dept_leader',
    'INDEX `idx_leave_dept_leader` (`dept_leader_id`)');
CALL flowable_add_index_if_not_exists('leave_request', 'idx_leave_upper_leader',
    'INDEX `idx_leave_upper_leader` (`upper_leader_id`)');

DROP PROCEDURE IF EXISTS `flowable_add_column_if_not_exists`;
DROP PROCEDURE IF EXISTS `flowable_add_index_if_not_exists`;

-- 注意:
-- 1. 既有用户不会自动分配部门, 需要在 sys_user 中补充 dept_id / position_id;
--    DataInitializer 会自动补全内置演示账号 (admin/manager/hr/zhangsan 等) 的组织信息。
-- 2. 升级后重启后端会自动部署新版本 BPMN; 旧版本运行中的实例仍使用旧定义。
