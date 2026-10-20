-- ============================================================
-- Flowable 工作流示例 - 数据库初始化脚本 (全量)
-- 执行: mysql -uroot -p < init.sql
--
-- 全新数据库请执行本脚本; 已有数据库升级请执行 upgrade-org.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `flowable_demo`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `flowable_demo`;

-- ------------------------------------------------------------
-- 部门表
-- 通过 parent_id 形成部门树; leader_id 指向该部门负责人
-- 请假流程根据申请人部门沿 parent_id 向上解析审批领导
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

-- ------------------------------------------------------------
-- 岗位表
-- position_level 用于岗位层级排序, 便于组织架构展示
-- ------------------------------------------------------------
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
-- 系统用户表 (业务表, Flowable 的表由引擎自动创建)
-- dept_id / position_id 关联组织架构
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
    `real_name`   VARCHAR(64)           DEFAULT NULL COMMENT '姓名',
    `role`        VARCHAR(32)           DEFAULT 'employee' COMMENT '角色: admin/executive/manager/hr/employee',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '所属部门ID',
    `position_id` BIGINT                DEFAULT NULL COMMENT '岗位ID',
    `email`       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT               DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `deleted`     TINYINT               DEFAULT 0 COMMENT '逻辑删除: 1删除 0正常',
    `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_position_id` (`position_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- ------------------------------------------------------------
-- 请假申请表
-- 审批人、部门、岗位均为提交时快照, 便于历史追溯
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `leave_request`
(
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `process_instance_id` VARCHAR(64)          DEFAULT NULL COMMENT '流程实例ID',
    `apply_user_id`       BIGINT               DEFAULT NULL COMMENT '申请人ID',
    `apply_user_name`     VARCHAR(64)          DEFAULT NULL COMMENT '申请人姓名',
    `dept_id`             BIGINT               DEFAULT NULL COMMENT '申请人部门ID(快照)',
    `dept_name`           VARCHAR(128)         DEFAULT NULL COMMENT '申请人部门名称(快照)',
    `position_id`         BIGINT               DEFAULT NULL COMMENT '申请人岗位ID(快照)',
    `position_name`       VARCHAR(128)         DEFAULT NULL COMMENT '申请人岗位名称(快照)',
    `dept_leader_id`      BIGINT               DEFAULT NULL COMMENT '部门领导用户ID(快照)',
    `dept_leader_name`    VARCHAR(64)          DEFAULT NULL COMMENT '部门领导姓名(快照)',
    `upper_leader_id`     BIGINT               DEFAULT NULL COMMENT '上级领导用户ID(快照, >3天时)',
    `upper_leader_name`   VARCHAR(64)          DEFAULT NULL COMMENT '上级领导姓名(快照, >3天时)',
    `days`                INT                  DEFAULT NULL COMMENT '请假天数',
    `start_date`          DATE                 DEFAULT NULL COMMENT '开始日期',
    `end_date`            DATE                 DEFAULT NULL COMMENT '结束日期',
    `reason`              VARCHAR(500)         DEFAULT NULL COMMENT '请假事由',
    `status`              TINYINT              DEFAULT 0 COMMENT '状态: 0审批中 1已通过 2已驳回',
    `deleted`             TINYINT              DEFAULT 0 COMMENT '逻辑删除',
    `create_time`         DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_time`         DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_process_instance` (`process_instance_id`),
    KEY `idx_apply_user` (`apply_user_id`),
    KEY `idx_leave_dept` (`dept_id`),
    KEY `idx_leave_dept_leader` (`dept_leader_id`),
    KEY `idx_leave_upper_leader` (`upper_leader_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='请假申请表';

-- ------------------------------------------------------------
-- 自定义表单定义表
-- form_key 与 BPMN 中 userTask / startEvent 的 flowable:formKey 对应
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `wf_form`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `form_key`    VARCHAR(64)  NOT NULL COMMENT '表单标识(绑定 BPMN flowable:formKey)',
    `name`        VARCHAR(128) NOT NULL COMMENT '表单名称',
    `category`    VARCHAR(64)           DEFAULT NULL COMMENT '分类',
    `schema_json` LONGTEXT              DEFAULT NULL COMMENT '字段定义 JSON',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `status`      TINYINT               DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `deleted`     TINYINT               DEFAULT 0 COMMENT '逻辑删除: 1删除 0正常',
    `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_form_key` (`form_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='自定义表单定义表';

-- 上传文件表
CREATE TABLE IF NOT EXISTS `sys_file`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name`   VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `content_type`  VARCHAR(128)          DEFAULT NULL COMMENT '文件类型',
    `file_size`     BIGINT                DEFAULT NULL COMMENT '文件大小(字节)',
    `storage_path`  VARCHAR(500) NOT NULL COMMENT '存储路径',
    `uploader_id`   BIGINT                DEFAULT NULL COMMENT '上传人ID',
    `uploader_name` VARCHAR(64)           DEFAULT NULL COMMENT '上传人姓名',
    `create_time`   DATETIME              DEFAULT NULL COMMENT '上传时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stored_name` (`stored_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='上传文件表';

-- 说明:
-- 1. 组织架构(部门/岗位/用户)由后端启动时 DataInitializer 幂等写入默认数据, 默认密码 123456
-- 2. 默认组织: 集团总部 -> 科技公司 -> 技术部/人事部/财务部
-- 3. 默认表单: leaveStartForm / leaveApprovalForm
-- 4. 已有数据库升级请执行 backend/sql/upgrade-org.sql
