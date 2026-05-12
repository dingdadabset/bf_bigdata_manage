-- ========================================================================
-- Migration: dga_platform2 → dga_platform (table structure alignment)
-- ========================================================================
-- Target: dga_platform2 database (MySQL 5.7, 172.20.85.39)
-- Source: dga_platform  database (10.0.20.107)
-- Notes:
--   1) This script is designed to be re-runnable.
--   2) It prefers additive / non-destructive alignment.
--   3) For risky narrowing changes (e.g. varchar(255) -> varchar(100)),
--      keep manual review instead of forcing silent truncation in migration.
-- 
-- How to run (USE mysql CLI, NOT GUI tools):
--   mysql -h 172.20.85.39 -u your_user -p dga_platform2 < migrate-dga_platform2.sql
-- ========================================================================

USE `dga_platform2`;

-- ========================================================================
-- Helper: Add column if not exists
-- ========================================================================
DROP PROCEDURE IF EXISTS `add_col`;
DELIMITER ;;
CREATE PROCEDURE `add_col`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_type VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND column_name = p_column;
    IF cnt = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_type);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Modify column if exists
-- ========================================================================
DROP PROCEDURE IF EXISTS `mod_col`;
DELIMITER ;;
CREATE PROCEDURE `mod_col`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND column_name = p_column;
    IF cnt > 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', p_def);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Add index if not exists
-- ========================================================================
DROP PROCEDURE IF EXISTS `add_idx`;
DELIMITER ;;
CREATE PROCEDURE `add_idx`(
    IN p_table VARCHAR(64),
    IN p_idx VARCHAR(64),
    IN p_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_idx;
    IF cnt = 0 THEN
        SET @s = CONCAT('CREATE INDEX `', p_idx, '` ON `', p_table, '` ', p_def);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Recreate index deterministically
-- ========================================================================
DROP PROCEDURE IF EXISTS `recreate_idx`;
DELIMITER ;;
CREATE PROCEDURE `recreate_idx`(
    IN p_table VARCHAR(64),
    IN p_idx VARCHAR(64),
    IN p_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_idx;
    IF cnt > 0 THEN
        SET @drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_idx, '`');
        PREPARE drop_stmt FROM @drop_sql;
        EXECUTE drop_stmt;
        DEALLOCATE PREPARE drop_stmt;
    END IF;
    SET @s = CONCAT('CREATE INDEX `', p_idx, '` ON `', p_table, '` ', p_def);
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Add unique key if not exists
-- ========================================================================
DROP PROCEDURE IF EXISTS `add_uk`;
DELIMITER ;;
CREATE PROCEDURE `add_uk`(
    IN p_table VARCHAR(64),
    IN p_uk VARCHAR(64),
    IN p_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND constraint_name = p_uk
      AND constraint_type = 'UNIQUE';
    IF cnt = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_uk, '` ', p_def);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Recreate unique key deterministically
-- ========================================================================
DROP PROCEDURE IF EXISTS `recreate_uk`;
DELIMITER ;;
CREATE PROCEDURE `recreate_uk`(
    IN p_table VARCHAR(64),
    IN p_uk VARCHAR(64),
    IN p_def VARCHAR(512)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_uk;
    IF cnt > 0 THEN
        SET @drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_uk, '`');
        PREPARE drop_stmt FROM @drop_sql;
        EXECUTE drop_stmt;
        DEALLOCATE PREPARE drop_stmt;
    END IF;
    SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_uk, '` ', p_def);
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END;;
DELIMITER ;

-- ========================================================================
-- Helper: Drop index if exists
-- ========================================================================
DROP PROCEDURE IF EXISTS `drop_idx`;
DELIMITER ;;
CREATE PROCEDURE `drop_idx`(
    IN p_table VARCHAR(64),
    IN p_idx VARCHAR(64)
)
BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE()
      AND table_name = p_table
      AND index_name = p_idx;
    IF cnt > 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_idx, '`');
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;;
DELIMITER ;


-- ========================================================================
-- Part 1: New tables that exist in dga_platform but not in dga_platform2
-- ========================================================================

CREATE TABLE IF NOT EXISTS `access_activity_evidence` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(100) DEFAULT NULL,
  `cluster_name` varchar(255) DEFAULT NULL,
  `collected_at` datetime(6) DEFAULT NULL,
  `confidence` varchar(20) DEFAULT NULL,
  `evidence` varchar(1000) DEFAULT NULL,
  `evidence_key` varchar(255) NOT NULL,
  `last_active_at` datetime(6) DEFAULT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `source_system` varchar(50) NOT NULL,
  `status` varchar(20) DEFAULT NULL,
  `username` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_access_activity_user` (`username`,`cluster_name`,`source_system`),
  KEY `idx_access_activity_source` (`source_system`,`collected_at`),
  KEY `idx_access_activity_key` (`evidence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `access_governance_issue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `access_id` bigint(20) DEFAULT NULL,
  `assignee` varchar(100) DEFAULT NULL,
  `cluster_code` varchar(100) DEFAULT NULL,
  `cluster_name` varchar(255) DEFAULT NULL,
  `database_name` varchar(255) DEFAULT NULL,
  `detected_at` datetime(6) DEFAULT NULL,
  `evidence` varchar(1000) DEFAULT NULL,
  `issue_key` varchar(255) NOT NULL,
  `issue_type` varchar(50) NOT NULL,
  `owner` varchar(100) DEFAULT NULL,
  `permission` varchar(100) DEFAULT NULL,
  `recommendation` varchar(1000) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` varchar(100) DEFAULT NULL,
  `resource_type` varchar(50) DEFAULT NULL,
  `severity` varchar(20) NOT NULL,
  `source_systems` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(100) DEFAULT NULL,
  `collaborator_owners` varchar(1000) DEFAULT NULL,
  `confidence` varchar(20) DEFAULT NULL,
  `last_active_at` datetime(6) DEFAULT NULL,
  `last_active_source` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_access_issue_status` (`status`,`severity`),
  KEY `idx_access_issue_user` (`username`,`cluster_name`),
  KEY `idx_access_issue_key` (`issue_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `access_owner` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `owner_code` varchar(100) NOT NULL,
  `source` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_access_owner_code` (`owner_code`),
  KEY `idx_access_owner_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `data_source_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `jdbc_url_template` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_t0102m6lefauayiultj30d2kb` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dga_column_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) NOT NULL,
  `column_type` varchar(255) NOT NULL,
  `comment_str` varchar(255) DEFAULT NULL,
  `is_primary_key` bit(1) DEFAULT NULL,
  `security_level` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dga_governance_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `handler` varchar(255) DEFAULT NULL,
  `issue_description` varchar(255) DEFAULT NULL,
  `issue_type` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL,
  `task_status` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dga_quality_issue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `first_seen_at` datetime(6) DEFAULT NULL,
  `issue_description` varchar(1000) DEFAULT NULL,
  `issue_title` varchar(255) DEFAULT NULL,
  `last_execution_id` bigint(20) DEFAULT NULL,
  `last_seen_at` datetime(6) DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `result_value` double DEFAULT NULL,
  `rule_id` bigint(20) NOT NULL,
  `severity` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL,
  `threshold` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dga_system_setting` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `scope` varchar(100) NOT NULL,
  `setting_group` varchar(100) NOT NULL,
  `setting_key` varchar(100) NOT NULL,
  `setting_value` longtext,
  `update_time` datetime(6) DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `value_type` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_setting_scope_group_key` (`scope`,`setting_group`,`setting_key`),
  KEY `idx_setting_scope_group` (`scope`,`setting_group`),
  KEY `idx_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dga_table_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `datasource_id` bigint(20) NOT NULL,
  `db_name` varchar(255) NOT NULL,
  `last_access_time` datetime(6) DEFAULT NULL,
  `location_path` varchar(255) DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `record_count` bigint(20) DEFAULT NULL,
  `storage_format` varchar(255) DEFAULT NULL,
  `table_name` varchar(255) NOT NULL,
  `total_size` bigint(20) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `meta_change_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `object_type` varchar(32) NOT NULL COMMENT '对象类型: DATABASE, TABLE, COLUMN, PARTITION',
  `object_name` varchar(256) NOT NULL COMMENT '对象全名 (db.table 或 db.table.col)',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型: CREATE, ALTER, DROP, OWNER_CHANGE, COMMENT_CHANGE',
  `old_value` text COMMENT '变更前内容 (JSON 格式)',
  `new_value` text COMMENT '变更后内容 (JSON 格式)',
  `operator` varchar(64) DEFAULT 'SYSTEM_SYNC' COMMENT '操作人/触发源',
  `occur_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更发生时间 (源端估算)',
  `discover_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '平台发现时间',
  `is_processed` tinyint(4) DEFAULT '0' COMMENT '是否已处理 (用于触发下游任务): 0-否, 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_discover_time` (`discover_time`),
  KEY `idx_object_name` (`object_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据变更审计日志表';

CREATE TABLE IF NOT EXISTS `meta_cluster_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_code` varchar(64) NOT NULL COMMENT '集群唯一编码 (如: CDH_PROD_01)',
  `cluster_name` varchar(128) NOT NULL COMMENT '集群显示名称',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型: CDH, HDP, CDP, AWS_GLUE, ALIYUN_MAXCOMPUTE',
  `platform_version` varchar(32) DEFAULT NULL COMMENT '平台版本 (如: CDH-6.3.2)',
  `hive_version` varchar(32) DEFAULT NULL COMMENT 'Hive 组件版本 (如: 2.1.1-cdh6.3.2)',
  `metastore_uri` varchar(256) DEFAULT NULL COMMENT 'Hive Metastore URI (thrift://...)',
  `jdbc_url` varchar(256) DEFAULT NULL COMMENT '直连元数据库 JDBC URL (仅限采集器内部使用)',
  `db_username` varchar(64) DEFAULT NULL COMMENT '元数据库用户名 (加密存储)',
  `db_password` varchar(256) DEFAULT NULL COMMENT '元数据库密码 (加密存储)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-停用, -1-异常',
  `sync_strategy` varchar(32) DEFAULT 'API' COMMENT '同步策略: API (推荐), DIRECT_DB',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后一次成功同步时间',
  `created_by` varchar(64) DEFAULT 'SYSTEM' COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_code` (`cluster_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群注册信息表';

CREATE TABLE IF NOT EXISTS `meta_column_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_id` bigint(20) NOT NULL COMMENT '关联表 ID (meta_table_std.id)',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID (冗余，便于分片)',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称 (冗余)',
  `table_name` varchar(128) NOT NULL COMMENT '表名称 (冗余)',
  `column_name` varchar(128) NOT NULL COMMENT '字段名称',
  `column_type` varchar(256) NOT NULL COMMENT '字段数据类型 (如: string, int, decimal(10,2))',
  `comment` varchar(1024) DEFAULT NULL COMMENT '字段注释/描述',
  `column_position` int(11) NOT NULL DEFAULT '0' COMMENT '字段顺序 (从 1 开始)',
  `is_partition_col` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否分区字段: 0-否, 1-是',
  `is_nullable` tinyint(4) DEFAULT '1' COMMENT '是否可空: 0-否, 1-是 (高版本 Hive 支持)',
  `default_value` varchar(256) DEFAULT NULL COMMENT '默认值 (高版本 Hive 支持)',
  `source_col_id` varchar(128) DEFAULT NULL COMMENT '源端列 ID',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_col` (`table_id`,`column_name`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_col_name` (`column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化字段信息表';

CREATE TABLE IF NOT EXISTS `meta_database_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID (meta_cluster_info.id)',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '数据库所有者',
  `owner_type` varchar(32) DEFAULT 'USER' COMMENT '所有者类型: USER, ROLE',
  `description` text COMMENT '数据库注释/描述',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT 'HDFS 存储根路径',
  `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (DB_PROPERTIES)',
  `source_db_id` varchar(128) DEFAULT NULL COMMENT '源端数据库 ID',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地更新时间',
  `connection_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_db` (`cluster_id`,`db_name`),
  KEY `idx_db_name` (`db_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化数据库信息表';

CREATE TABLE IF NOT EXISTS `meta_enhanced_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_id` bigint(20) DEFAULT NULL,
  `column_count` int(11) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `total_size` bigint(20) DEFAULT NULL,
  `db_name` varchar(128) DEFAULT NULL,
  `description` text,
  `storage_format` varchar(50) DEFAULT NULL,
  `last_access_time` datetime(6) DEFAULT NULL,
  `location_uri` varchar(1024) DEFAULT NULL,
  `original_table_id` bigint(20) DEFAULT NULL,
  `owner_name` varchar(128) DEFAULT NULL,
  `partition_count` int(11) DEFAULT NULL,
  `record_count` bigint(20) DEFAULT NULL,
  `sync_time` datetime(6) DEFAULT NULL,
  `table_name` varchar(128) DEFAULT NULL,
  `table_type` varchar(32) DEFAULT NULL,
  `partition_keys` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `meta_partition_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_id` bigint(20) NOT NULL COMMENT '关联表 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称',
  `table_name` varchar(128) NOT NULL COMMENT '表名称',
  `partition_name` varchar(256) NOT NULL COMMENT '分区名称 (如: dt=20231001/region=cn)',
  `partition_values` json DEFAULT NULL COMMENT '分区键值对解析',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT '分区存储路径',
  `create_time` datetime DEFAULT NULL COMMENT '分区创建时间',
  `last_access_time` datetime DEFAULT NULL COMMENT '最后访问时间',
  `parameters_json` json DEFAULT NULL COMMENT '分区扩展参数',
  `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '分区数据大小',
  `row_count` bigint(20) DEFAULT '0' COMMENT '分区行数',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_part` (`table_id`,`partition_name`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化分区信息表';

CREATE TABLE IF NOT EXISTS `meta_table_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称',
  `table_name` varchar(128) NOT NULL COMMENT '表名称',
  `table_type` varchar(32) NOT NULL COMMENT '表类型: MANAGED_TABLE, EXTERNAL_TABLE, VIRTUAL_VIEW, MATERIALIZED_VIEW',
  `format_type` varchar(64) DEFAULT NULL COMMENT '文件格式: ORC, PARQUET, AVRO, TEXTFILE, SEQUENCEFILE',
  `input_format` varchar(256) DEFAULT NULL COMMENT 'InputFormat 类名',
  `output_format` varchar(256) DEFAULT NULL COMMENT 'OutputFormat 类名',
  `serde_class` varchar(256) DEFAULT NULL COMMENT 'SerDe 序列化类名',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT '数据存储路径',
  `create_time` datetime DEFAULT NULL COMMENT '表创建时间',
  `last_ddl_time` datetime DEFAULT NULL COMMENT '最后 DDL 变更时间',
  `retention` int(11) DEFAULT '0' COMMENT '数据保留时间 (天)',
  `view_expanded_text` longtext COMMENT '视图展开后的 SQL',
  `view_original_text` longtext COMMENT '视图原始创建 SQL',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '表所有者',
  `description` text COMMENT '表注释/描述',
  `is_partitioned` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否分区表: 0-否, 1-是',
  `partition_keys` json DEFAULT NULL COMMENT '分区字段列表',
  `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (TBLPROPERTIES)',
  `source_tbl_id` varchar(128) DEFAULT NULL COMMENT '源端表 ID',
  `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '数据总大小 (字节)',
  `row_count` bigint(20) DEFAULT '0' COMMENT '预估行数',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  `update_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地记录更新时间',
  `partition_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_db_tbl` (`cluster_id`,`db_name`,`table_name`),
  KEY `idx_db_table` (`db_name`,`table_name`),
  KEY `idx_format_type` (`format_type`),
  KEY `idx_last_ddl` (`last_ddl_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化表信息表';


-- ========================================================================
-- Part 2: ALTER existing tables - add missing columns
-- ========================================================================

-- 2.1 data_source_config
CALL add_col('data_source_config', 'created_at', 'datetime(6) DEFAULT NULL AFTER `id`');
CALL add_col('data_source_config', 'is_deleted', 'bit(1) DEFAULT NULL AFTER `created_at`');
CALL add_col('data_source_config', 'description', 'varchar(255) DEFAULT NULL AFTER `username`');
CALL add_col('data_source_config', 'updated_by', 'varchar(255) DEFAULT NULL AFTER `description`');
CALL add_col('data_source_config', 'created_by', 'varchar(255) DEFAULT NULL AFTER `updated_by`');
CALL add_col('data_source_config', 'cluster_code', 'varchar(255) DEFAULT NULL AFTER `created_by`');
CALL add_col('data_source_config', 'cluster_name', 'varchar(255) DEFAULT NULL AFTER `cluster_code`');
CALL add_col('data_source_config', 'endpoint_id', 'bigint(20) DEFAULT NULL AFTER `cluster_name`');
CALL add_col('data_source_config', 'last_sync_message', 'varchar(1000) DEFAULT NULL AFTER `endpoint_id`');
CALL add_col('data_source_config', 'last_sync_status', 'varchar(255) DEFAULT NULL AFTER `last_sync_message`');
CALL add_col('data_source_config', 'last_sync_time', 'datetime(6) DEFAULT NULL AFTER `last_sync_status`');
CALL add_col('data_source_config', 'status', 'varchar(255) DEFAULT NULL AFTER `last_sync_time`');
CALL add_col('data_source_config', 'updated_at', 'datetime(6) DEFAULT NULL AFTER `type`');

CALL mod_col('data_source_config', 'created_at', 'datetime(6) DEFAULT NULL');
CALL mod_col('data_source_config', 'is_deleted', 'bit(1) DEFAULT NULL');
CALL mod_col('data_source_config', 'updated_at', 'datetime(6) DEFAULT NULL');
CALL mod_col('data_source_config', 'description', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'updated_by', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'created_by', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'cluster_code', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'cluster_name', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'endpoint_id', 'bigint(20) DEFAULT NULL');
CALL mod_col('data_source_config', 'last_sync_message', 'varchar(1000) DEFAULT NULL');
CALL mod_col('data_source_config', 'last_sync_status', 'varchar(255) DEFAULT NULL');
CALL mod_col('data_source_config', 'last_sync_time', 'datetime(6) DEFAULT NULL');
CALL mod_col('data_source_config', 'status', 'varchar(255) DEFAULT NULL');

-- 2.2 dga_access_log
CALL add_col('dga_access_log', 'created_at', 'datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER `error_message`');
CALL add_col('dga_access_log', 'update_time', 'datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER `created_at`');
CALL add_col('dga_access_log', 'is_deleted', 'tinyint(1) NOT NULL DEFAULT 0 AFTER `update_time`');

CALL mod_col('dga_access_log', 'error_message', 'text');
CALL mod_col('dga_access_log', 'created_at', 'datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');
CALL mod_col('dga_access_log', 'update_time', 'datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL mod_col('dga_access_log', 'is_deleted', 'tinyint(1) NOT NULL DEFAULT 0');

-- 2.3 dga_cluster
CALL add_col('dga_cluster', 'created_at', 'datetime(6) DEFAULT NULL AFTER `cluster_name`');
CALL add_col('dga_cluster', 'is_deleted', 'bit(1) DEFAULT NULL AFTER `created_at`');
CALL add_col('dga_cluster', 'updated_at', 'datetime(6) DEFAULT NULL AFTER `is_deleted`');
CALL add_col('dga_cluster', 'hive_password', 'varchar(255) DEFAULT NULL AFTER `type`');
CALL add_col('dga_cluster', 'hive_url', 'varchar(255) DEFAULT NULL AFTER `hive_password`');
CALL add_col('dga_cluster', 'hive_username', 'varchar(255) DEFAULT NULL AFTER `hive_url`');
CALL add_col('dga_cluster', 'ldap_base', 'varchar(255) DEFAULT NULL AFTER `hive_username`');
CALL add_col('dga_cluster', 'ldap_password', 'varchar(255) DEFAULT NULL AFTER `ldap_base`');
CALL add_col('dga_cluster', 'ldap_urls', 'varchar(255) DEFAULT NULL AFTER `ldap_password`');
CALL add_col('dga_cluster', 'ldap_username', 'varchar(255) DEFAULT NULL AFTER `ldap_urls`');
CALL add_col('dga_cluster', 'ldap_user_base', 'varchar(255) DEFAULT NULL AFTER `ldap_username`');
CALL add_col('dga_cluster', 'cluster_code', 'varchar(255) DEFAULT NULL AFTER `ldap_user_base`');

-- 2.4 dga_cluster_endpoint
CALL add_col('dga_cluster_endpoint', 'driver_profile', 'varchar(255) DEFAULT NULL AFTER `username`');
CALL add_col('dga_cluster_endpoint', 'driver_key', 'varchar(255) DEFAULT NULL AFTER `driver_profile`');

-- 2.5 dga_quality_execution
CALL add_col('dga_quality_execution', 'duration_ms', 'bigint(20) DEFAULT NULL AFTER `status`');
CALL add_col('dga_quality_execution', 'executed_by', 'varchar(255) DEFAULT NULL AFTER `duration_ms`');
CALL add_col('dga_quality_execution', 'executed_sql', 'varchar(4000) DEFAULT NULL AFTER `executed_by`');
CALL add_col('dga_quality_execution', 'scan_filter', 'varchar(1000) DEFAULT NULL AFTER `executed_sql`');
CALL add_col('dga_quality_execution', 'scan_scope', 'varchar(255) DEFAULT NULL AFTER `scan_filter`');
CALL add_col('dga_quality_execution', 'table_id', 'bigint(20) DEFAULT NULL AFTER `scan_scope`');
CALL add_col('dga_quality_execution', 'threshold', 'double DEFAULT NULL AFTER `table_id`');

-- 2.6 dga_quality_rule
CALL add_col('dga_quality_rule', 'created_by', 'varchar(255) DEFAULT NULL AFTER `threshold`');
CALL add_col('dga_quality_rule', 'datasource_id', 'bigint(20) DEFAULT NULL AFTER `created_by`');
CALL add_col('dga_quality_rule', 'db_name', 'varchar(255) DEFAULT NULL AFTER `datasource_id`');
CALL add_col('dga_quality_rule', 'expected_value', 'varchar(255) DEFAULT NULL AFTER `db_name`');
CALL add_col('dga_quality_rule', 'last_error_message', 'varchar(1000) DEFAULT NULL AFTER `expected_value`');
CALL add_col('dga_quality_rule', 'last_executed_at', 'datetime(6) DEFAULT NULL AFTER `last_error_message`');
CALL add_col('dga_quality_rule', 'last_execution_status', 'varchar(255) DEFAULT NULL AFTER `last_executed_at`');
CALL add_col('dga_quality_rule', 'last_result_value', 'double DEFAULT NULL AFTER `last_execution_status`');
CALL add_col('dga_quality_rule', 'max_value', 'double DEFAULT NULL AFTER `last_result_value`');
CALL add_col('dga_quality_rule', 'min_value', 'double DEFAULT NULL AFTER `max_value`');
CALL add_col('dga_quality_rule', 'owner', 'varchar(255) DEFAULT NULL AFTER `min_value`');
CALL add_col('dga_quality_rule', 'regex_pattern', 'varchar(255) DEFAULT NULL AFTER `owner`');
CALL add_col('dga_quality_rule', 'rule_name', 'varchar(255) DEFAULT NULL AFTER `regex_pattern`');
CALL add_col('dga_quality_rule', 'scan_scope', 'varchar(255) DEFAULT NULL AFTER `rule_name`');
CALL add_col('dga_quality_rule', 'severity', 'varchar(255) DEFAULT NULL AFTER `scan_scope`');
CALL add_col('dga_quality_rule', 'status', 'varchar(255) DEFAULT NULL AFTER `severity`');
CALL add_col('dga_quality_rule', 'table_name', 'varchar(255) DEFAULT NULL AFTER `status`');
CALL add_col('dga_quality_rule', 'updated_at', 'datetime(6) DEFAULT NULL AFTER `table_name`');

-- 2.7 dga_resource_link
CALL add_col('dga_resource_link', 'created_at', 'datetime DEFAULT CURRENT_TIMESTAMP AFTER `sort_order`');
CALL add_col('dga_resource_link', 'updated_at', 'datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`');

-- 2.8 dga_table_score
CALL add_col('dga_table_score', 'governance_advice', 'varchar(255) DEFAULT NULL AFTER `id`');
CALL add_col('dga_table_score', 'score_cost', 'decimal(19,2) DEFAULT NULL AFTER `report_date`');
CALL add_col('dga_table_score', 'score_quality', 'decimal(19,2) DEFAULT NULL AFTER `score_cost`');
CALL add_col('dga_table_score', 'score_security', 'decimal(19,2) DEFAULT NULL AFTER `score_quality`');
CALL add_col('dga_table_score', 'score_spec', 'decimal(19,2) DEFAULT NULL AFTER `score_security`');

-- 2.9 dga_user_recent_views
CALL add_col('dga_user_recent_views', 'datasource_id', 'bigint(20) DEFAULT NULL AFTER `viewed_at`');
CALL add_col('dga_user_recent_views', 'resource_id', 'bigint(20) DEFAULT NULL AFTER `datasource_id`');

-- 2.10 dga_users
CALL add_col('dga_users', 'is_protected', 'bit(1) DEFAULT NULL AFTER `cluster_name`');
CALL add_col('dga_users', 'expires_at', 'datetime(6) DEFAULT NULL AFTER `is_protected`');
CALL add_col('dga_users', 'last_active_at', 'datetime(6) DEFAULT NULL AFTER `expires_at`');
CALL add_col('dga_users', 'last_active_source', 'varchar(255) DEFAULT NULL AFTER `last_active_at`');
CALL add_col('dga_users', 'user_type', 'varchar(255) DEFAULT NULL AFTER `last_active_source`');

UPDATE `dga_users` SET `cluster_name` = 'CDH' WHERE `cluster_name` IS NULL OR `cluster_name` = '';
CALL mod_col('dga_users', 'cluster_name', 'varchar(255) NOT NULL DEFAULT ''CDH''');
CALL mod_col('dga_users', 'is_deleted', 'int(11) NOT NULL DEFAULT 0');
CALL mod_col('dga_users', 'update_time', 'datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');
CALL mod_col('dga_users', 'is_protected', 'bit(1) DEFAULT NULL');
CALL mod_col('dga_users', 'expires_at', 'datetime(6) DEFAULT NULL');
CALL mod_col('dga_users', 'last_active_at', 'datetime(6) DEFAULT NULL');
CALL mod_col('dga_users', 'last_active_source', 'varchar(255) DEFAULT NULL');
CALL mod_col('dga_users', 'user_type', 'varchar(255) DEFAULT NULL');

-- 2.11 meta_column_info
CALL add_col('meta_column_info', 'column_comment', 'varchar(255) DEFAULT NULL AFTER `column_type`');
CALL add_col('meta_column_info', 'is_partition_key', 'bit(1) DEFAULT NULL AFTER `column_comment`');
CALL add_col('meta_column_info', 'sort_order', 'int(11) DEFAULT NULL AFTER `is_partition_key`');
CALL add_col('meta_column_info', 'comment_str', 'varchar(255) DEFAULT NULL AFTER `data_type`');
CALL add_col('meta_column_info', 'comment', 'varchar(255) DEFAULT NULL AFTER `security_level`');

-- 2.12 meta_table_info
CALL add_col('meta_table_info', 'file_count', 'bigint(20) DEFAULT NULL AFTER `db_name`');
CALL add_col('meta_table_info', 'status', 'varchar(255) DEFAULT NULL AFTER `storage_format`');
CALL add_col('meta_table_info', 'table_type', 'varchar(255) DEFAULT NULL AFTER `status`');
CALL add_col('meta_table_info', 'datasource_name', 'varchar(255) DEFAULT NULL AFTER `table_type`');
CALL add_col('meta_table_info', 'create_time', 'datetime(6) DEFAULT NULL AFTER `datasource_name`');
CALL add_col('meta_table_info', 'dw_level', 'varchar(255) DEFAULT NULL AFTER `create_time`');
CALL add_col('meta_table_info', 'governance_score', 'decimal(19,2) DEFAULT NULL AFTER `dw_level`');
CALL add_col('meta_table_info', 'is_partitioned', 'bit(1) DEFAULT NULL AFTER `governance_score`');
CALL add_col('meta_table_info', 'lifecycle_days', 'int(11) DEFAULT NULL AFTER `is_partitioned`');
CALL add_col('meta_table_info', 'location_path', 'varchar(255) DEFAULT NULL AFTER `lifecycle_days`');
CALL add_col('meta_table_info', 'total_size', 'bigint(20) DEFAULT NULL AFTER `location_path`');
CALL add_col('meta_table_info', 'updated_at', 'datetime(6) DEFAULT NULL AFTER `total_size`');
CALL add_col('meta_table_info', 'cluster_code', 'varchar(255) DEFAULT NULL AFTER `updated_at`');
CALL add_col('meta_table_info', 'owner_source', 'varchar(255) DEFAULT NULL AFTER `cluster_code`');
CALL add_col('meta_table_info', 'source_owner', 'varchar(255) DEFAULT NULL AFTER `owner_source`');
CALL add_col('meta_table_info', 'lifecycle_status', 'varchar(255) DEFAULT NULL AFTER `source_owner`');

CALL mod_col('meta_table_info', 'governance_score', 'decimal(19,2) DEFAULT NULL');
CALL mod_col('meta_table_info', 'cluster_code', 'varchar(255) DEFAULT NULL');
CALL mod_col('meta_table_info', 'owner_source', 'varchar(255) DEFAULT NULL');
CALL mod_col('meta_table_info', 'source_owner', 'varchar(255) DEFAULT NULL');
CALL mod_col('meta_table_info', 'lifecycle_status', 'varchar(255) DEFAULT NULL');
CALL mod_col('meta_table_info', 'updated_at', 'datetime(6) DEFAULT NULL');

-- 2.13 user_resource_access
CALL add_col('user_resource_access', 'owner', 'varchar(255) DEFAULT NULL AFTER `username`');
CALL add_col('user_resource_access', 'collaborator_owners', 'varchar(1000) DEFAULT NULL AFTER `owner`');
CALL add_col('user_resource_access', 'last_reviewed_at', 'datetime(6) DEFAULT NULL AFTER `collaborator_owners`');
CALL add_col('user_resource_access', 'reviewed_by', 'varchar(255) DEFAULT NULL AFTER `last_reviewed_at`');
CALL add_col('user_resource_access', 'review_due_at', 'datetime(6) DEFAULT NULL AFTER `reviewed_by`');
CALL add_col('user_resource_access', 'revoked_by', 'varchar(255) DEFAULT NULL AFTER `review_due_at`');

-- 2.14 users
CALL add_col('users', 'mobile', 'varchar(20) DEFAULT NULL AFTER `update_time`');
CALL add_col('users', 'open_id', 'varchar(100) DEFAULT NULL AFTER `mobile`');
CALL add_col('users', 'provider', 'varchar(20) DEFAULT NULL AFTER `open_id`');


-- ========================================================================
-- Part 3: Add missing indexes and constraints
-- ========================================================================

-- 3.1 data_source_config
CALL recreate_idx('data_source_config', 'idx_ds_search_scope', '(`type`, `endpoint_id`, `is_deleted`, `id`)');

-- 3.2 dga_access_log
CALL recreate_idx('dga_access_log', 'idx_access_log_user_time', '(`username`, `created_at`)');
CALL recreate_idx('dga_access_log', 'idx_access_log_type_status_time', '(`request_type`, `status`, `created_at`)');
CALL recreate_idx('dga_access_log', 'idx_access_log_status_time', '(`status`, `created_at`)');
CALL recreate_idx('dga_access_log', 'idx_access_log_deleted_time', '(`is_deleted`, `created_at`)');

-- 3.3 dga_resource_link
CALL recreate_uk('dga_resource_link', 'uk_resource_url', '(`url`)');
CALL recreate_idx('dga_resource_link', 'idx_category_env', '(`category`, `env`)');
CALL recreate_idx('dga_resource_link', 'idx_recommended', '(`recommended`)');

-- 3.4 dga_users: align with latest dump, keep both unique keys
CALL drop_idx('dga_users', 'uk_username');
CALL recreate_uk('dga_users', 'UK4no4piterbhq6h4hjmqnp56e1', '(`username`, `cluster_name`)');
CALL recreate_uk('dga_users', 'uk_cluster_username', '(`cluster_name`, `username`)');

-- 3.5 meta_table_info: search indexes
-- Use prefix lengths for utf8mb4 to stay within InnoDB 3072-byte index limit.
CALL recreate_idx('meta_table_info', 'idx_meta_search_filters', '(`datasource_id`, `db_name`(128), `owner`(64), `lifecycle_status`(32), `sync_time`)');
CALL recreate_idx('meta_table_info', 'idx_meta_source_owner', '(`source_owner`)');
CALL recreate_idx('meta_table_info', 'idx_meta_table_name', '(`table_name`)');

-- 3.6 meta_column_info
CALL recreate_idx('meta_column_info', 'idx_col_name_table', '(`column_name`, `table_id`)');

-- 3.7 user_resource_access
CALL recreate_idx('user_resource_access', 'idx_user_resource_owner', '(`owner`, `status`)');
CALL recreate_idx('user_resource_access', 'idx_user_resource_review_due', '(`review_due_at`, `status`)');


-- ========================================================================
-- Cleanup: Drop helper procedures
-- ========================================================================
DROP PROCEDURE IF EXISTS `add_col`;
DROP PROCEDURE IF EXISTS `mod_col`;
DROP PROCEDURE IF EXISTS `add_idx`;
DROP PROCEDURE IF EXISTS `recreate_idx`;
DROP PROCEDURE IF EXISTS `add_uk`;
DROP PROCEDURE IF EXISTS `recreate_uk`;
DROP PROCEDURE IF EXISTS `drop_idx`;
