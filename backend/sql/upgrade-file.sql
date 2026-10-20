-- ============================================================
-- 既有数据库升级脚本: 新增文件上传表
-- 执行: mysql -uroot -p flowable_demo < upgrade-file.sql
-- 可重复执行
-- ============================================================

USE `flowable_demo`;

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
