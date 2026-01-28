-- Active: 1769414533847@@127.0.0.1@3306@dga_platform
-- DGA Platform Database Schema Reference
-- This file is for reference only. Spring Boot (JPA) will automatically create these tables.

-- 1. Data Source Configuration
CREATE TABLE IF NOT EXISTS `data_source_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(50) NOT NULL COMMENT 'HIVE, STARROCKS, MYSQL',
  `url` VARCHAR(500) NOT NULL,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Metadata: Table Info
CREATE TABLE IF NOT EXISTS `dga_table_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `datasource_id` BIGINT NOT NULL COMMENT 'FK to data_source_config.id',
  `db_name` VARCHAR(100) NOT NULL,
  `table_name` VARCHAR(100) NOT NULL,
  `owner` VARCHAR(100),
  `storage_format` VARCHAR(50) COMMENT 'ORC, PARQUET, TEXTFILE',
  `location_path` VARCHAR(500),
  `total_size` BIGINT COMMENT 'in bytes',
  `record_count` BIGINT,
  `last_access_time` DATETIME,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_ds_db_tbl` (`datasource_id`, `db_name`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Metadata: Column Info
CREATE TABLE IF NOT EXISTS `dga_column_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL COMMENT 'FK to dga_table_metadata.id',
  `column_name` VARCHAR(100) NOT NULL,
  `column_type` VARCHAR(50) NOT NULL,
  `comment_str` TEXT,
  `is_primary_key` BOOLEAN DEFAULT FALSE,
  `security_level` VARCHAR(20) COMMENT 'L1, L2, L3, L4',
  PRIMARY KEY (`id`),
  INDEX `idx_table_col` (`table_id`, `column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Data Quality Rules
CREATE TABLE IF NOT EXISTS `dga_quality_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `column_name` VARCHAR(100),
  `rule_type` VARCHAR(50) NOT NULL COMMENT 'NULL_CHECK, UNIQUENESS, etc.',
  `threshold` DOUBLE COMMENT 'Failure rate threshold e.g. 0.05',
  `action_type` VARCHAR(50) COMMENT 'ALARM, BLOCK_JOB',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Data Quality Executions
CREATE TABLE IF NOT EXISTS `dga_quality_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAILED, WARNING',
  `result_value` DOUBLE,
  `error_message` TEXT,
  `executed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_rule_exec` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Access Management Logs
CREATE TABLE IF NOT EXISTS `dga_access_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `request_type` VARCHAR(50) NOT NULL COMMENT 'GRANT_HIVE, CREATE_USER',
  `target_resource` VARCHAR(200),
  `permission_granted` VARCHAR(50),
  `status` VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAILED',
  `error_message` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. User Info
CREATE TABLE IF NOT EXISTS `dga_users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255),
  `email` VARCHAR(255),
  `first_name` VARCHAR(100),
  `last_name` VARCHAR(100),
  `creation_strategy` VARCHAR(50) COMMENT 'LDAP, IPA_SSH, IPA_HTTP, SELF_REGISTER',
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Data Map: Recent Views (Enhanced)
CREATE TABLE IF NOT EXISTS `dga_user_recent_views` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `view_type` VARCHAR(50) NOT NULL COMMENT 'TABLE, DATASOURCE, DATABASE, COLUMN',
  `view_content` VARCHAR(500) NOT NULL COMMENT 'e.g. db_name.table_name',
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id',
  `viewed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_view` (`username`, `viewed_at` DESC),
  INDEX `idx_datasource` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户最近浏览记录';

-- 9. Data Map: Search History
CREATE TABLE IF NOT EXISTS `dga_search_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `search_query` VARCHAR(500) NOT NULL,
  `search_type` VARCHAR(50) NOT NULL COMMENT 'KEYWORD, AI, ADVANCED',
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id',
  `result_count` INT DEFAULT 0,
  `searched_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_search` (`username`, `searched_at` DESC),
  INDEX `idx_query` (`search_query`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户搜索历史';

-- 10. Data Map: User Favorites
CREATE TABLE IF NOT EXISTS `dga_user_favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `resource_type` VARCHAR(50) NOT NULL COMMENT 'TABLE, DATASOURCE, DATABASE',
  `resource_id` BIGINT NOT NULL,
  `resource_name` VARCHAR(500) NOT NULL,
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id',
  `tags` VARCHAR(500) COMMENT 'comma-separated',
  `notes` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_resource` (`username`, `resource_type`, `resource_id`),
  INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏夹';

-- 11. Data Map: Statistics Cache
CREATE TABLE IF NOT EXISTS `dga_datamap_stats_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `datasource_id` BIGINT COMMENT 'NULL for global',
  `stat_type` VARCHAR(50) NOT NULL COMMENT 'INSTANCE, DATABASE, TABLE, API, COLLECTOR',
  `stat_name` VARCHAR(100) NOT NULL,
  `stat_value` BIGINT NOT NULL,
  `calculated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_datasource_stat` (`datasource_id`, `stat_type`, `stat_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计信息缓存';

-- For additional Data Map tables (categories, lineage, etc.), see schema-datamap.sql
