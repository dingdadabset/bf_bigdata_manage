-- Active: 1769742324601@@10.0.20.107@3306@dga_platform
-- DGA Platform Database Schema Reference
-- This file is for reference only. Spring Boot (JPA) will automatically create these tables.

-- 1. Data Source Configuration
CREATE TABLE IF NOT EXISTS `data_source_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `type` VARCHAR(50) NOT NULL COMMENT 'HIVE, STARROCKS, MYSQL',
  `cluster_code` VARCHAR(100),
  `cluster_name` VARCHAR(255),
  `endpoint_id` BIGINT,
  `url` VARCHAR(500) NOT NULL,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `status` VARCHAR(20),
  `description` VARCHAR(500),
  `last_sync_time` DATETIME,
  `last_sync_status` VARCHAR(20),
  `last_sync_message` VARCHAR(1000),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_endpoint_type` (`cluster_code`, `endpoint_id`, `type`),
  INDEX `idx_ds_search_scope` (`type`, `endpoint_id`, `is_deleted`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Metadata: Table Info
CREATE TABLE IF NOT EXISTS `meta_table_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `datasource_id` BIGINT NOT NULL COMMENT 'FK to data_source_config.id',
  `cluster_code` VARCHAR(100),
  `db_name` VARCHAR(100) NOT NULL,
  `table_name` VARCHAR(100) NOT NULL,
  `table_comment` VARCHAR(1000),
  `source_owner` VARCHAR(100),
  `owner` VARCHAR(100),
  `owner_source` VARCHAR(50) COMMENT 'HIVE, MANUAL',
  `storage_format` VARCHAR(50) COMMENT 'ORC, PARQUET, TEXTFILE',
  `hdfs_path` VARCHAR(500),
  `table_size` BIGINT COMMENT 'in bytes',
  `record_count` BIGINT,
  `partition_count` BIGINT DEFAULT 0,
  `lifecycle_status` VARCHAR(30) DEFAULT 'ONLINE' COMMENT 'ONLINE, DEPRECATED, OFFLINE',
  `last_access_time` DATETIME,
  `last_modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `governance_score` DOUBLE,
  `sync_time` DATETIME,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_db_tbl` (`datasource_id`, `db_name`, `table_name`),
  INDEX `idx_cluster_db_tbl` (`cluster_code`, `db_name`, `table_name`),
  INDEX `idx_meta_search_filters` (`datasource_id`, `db_name`, `owner`, `lifecycle_status`, `sync_time`),
  INDEX `idx_meta_source_owner` (`source_owner`),
  INDEX `idx_meta_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Metadata: Column Info
CREATE TABLE IF NOT EXISTS `meta_column_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL COMMENT 'FK to meta_table_info.id',
  `column_name` VARCHAR(100) NOT NULL,
  `column_type` VARCHAR(50) NOT NULL,
  `data_type` VARCHAR(100),
  `column_comment` TEXT,
  `is_primary_key` BOOLEAN DEFAULT FALSE,
  `security_level` VARCHAR(20) COMMENT 'L1, L2, L3, L4',
  PRIMARY KEY (`id`),
  INDEX `idx_table_col` (`table_id`, `column_name`),
  INDEX `idx_col_name_table` (`column_name`, `table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `meta_partition_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL COMMENT 'FK to meta_table_info.id',
  `datasource_id` BIGINT,
  `cluster_code` VARCHAR(100),
  `db_name` VARCHAR(100),
  `table_name` VARCHAR(100),
  `partition_name` VARCHAR(500) NOT NULL COMMENT 'e.g. dt=2026-04-27/city=sh',
  `partition_spec` VARCHAR(1000),
  `hdfs_path` VARCHAR(1000),
  `storage_format` VARCHAR(50),
  `table_size` BIGINT,
  `record_count` BIGINT,
  `last_access_time` DATETIME,
  `last_modify_time` DATETIME,
  `sync_time` DATETIME,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_partition` (`table_id`, `partition_name`),
  INDEX `idx_partition_table` (`table_id`, `last_modify_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hive 最新分区元数据';

CREATE TABLE IF NOT EXISTS `dga_data_theme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `theme_name` VARCHAR(100) NOT NULL,
  `parent_id` BIGINT,
  `description` TEXT,
  `sort_order` INT DEFAULT 0,
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_theme_parent` (`parent_id`),
  INDEX `idx_theme_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据主题';

CREATE TABLE IF NOT EXISTS `dga_table_business_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `theme_id` BIGINT,
  `business_description` TEXT,
  `business_definition` TEXT,
  `business_owner` VARCHAR(100),
  `updated_by` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_table` (`table_id`),
  INDEX `idx_business_theme` (`theme_id`),
  INDEX `idx_business_owner` (`business_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表业务元数据';

CREATE TABLE IF NOT EXISTS `dga_metadata_context_suggestion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `column_id` BIGINT,
  `data_source_id` BIGINT,
  `source_type` VARCHAR(50) NOT NULL,
  `source_endpoint_id` BIGINT NOT NULL,
  `project_name` VARCHAR(255),
  `flow_name` VARCHAR(255),
  `job_name` VARCHAR(255),
  `context_type` VARCHAR(50) NOT NULL COMMENT 'COLUMN_COMMENT, SCHEDULER_CONTEXT',
  `suggested_value` TEXT,
  `evidence` TEXT,
  `confidence` VARCHAR(20),
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, APPLIED, REJECTED',
  `run_id` VARCHAR(100),
  `parsed_at` DATETIME,
  `applied_at` DATETIME,
  `applied_by` VARCHAR(100),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_context_suggestion_table` (`table_id`, `status`, `context_type`),
  INDEX `idx_context_suggestion_column` (`column_id`, `status`),
  INDEX `idx_context_suggestion_source` (`source_endpoint_id`, `data_source_id`, `run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据上下文建议';

CREATE TABLE IF NOT EXISTS `dga_metric_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `metric_name` VARCHAR(255) NOT NULL,
  `metric_code` VARCHAR(255) NOT NULL,
  `business_definition` TEXT,
  `calculation_logic` TEXT,
  `table_id` BIGINT,
  `owner` VARCHAR(100),
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_metric_code` (`metric_code`),
  INDEX `idx_metric_table` (`table_id`),
  INDEX `idx_metric_status` (`status`),
  INDEX `idx_metric_table_status` (`table_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标定义';

CREATE TABLE IF NOT EXISTS `dga_metadata_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(100) NOT NULL,
  `tag_type` VARCHAR(50) DEFAULT 'CUSTOM',
  `color` VARCHAR(30),
  `description` VARCHAR(500),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据标签';

CREATE TABLE IF NOT EXISTS `dga_table_tag_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  `assigned_by` VARCHAR(100),
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_tag` (`table_id`, `tag_id`),
  INDEX `idx_tag_table` (`tag_id`, `table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表标签关系';

INSERT IGNORE INTO `dga_data_theme` (`id`, `theme_name`, `parent_id`, `description`, `sort_order`, `status`) VALUES
(1, '交易主题', NULL, '订单、支付、退款等交易数据', 10, 'ACTIVE'),
(2, '用户主题', NULL, '账号、用户画像、行为等用户数据', 20, 'ACTIVE'),
(3, '运营主题', NULL, '活动、渠道、增长等运营分析数据', 30, 'ACTIVE'),
(4, '风控主题', NULL, '风险识别、审计、安全相关数据', 40, 'ACTIVE');

CREATE TABLE IF NOT EXISTS `dga_metadata_collection_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `datasource_id` BIGINT NOT NULL,
  `datasource_name` VARCHAR(255),
  `cluster_code` VARCHAR(100),
  `trigger_type` VARCHAR(50),
  `triggered_by` VARCHAR(100),
  `status` VARCHAR(20),
  `started_at` DATETIME,
  `finished_at` DATETIME,
  `success_table_count` INT DEFAULT 0,
  `failed_table_count` INT DEFAULT 0,
  `message` VARCHAR(1000),
  `error_detail` TEXT,
  PRIMARY KEY (`id`),
  INDEX `idx_collect_datasource` (`datasource_id`, `started_at`),
  INDEX `idx_collect_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据采集任务';

-- 4. Data Quality Rules
CREATE TABLE IF NOT EXISTS `dga_quality_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `datasource_id` BIGINT,
  `db_name` VARCHAR(255),
  `table_name` VARCHAR(255),
  `rule_name` VARCHAR(255),
  `column_name` VARCHAR(100),
  `rule_type` VARCHAR(50) NOT NULL COMMENT 'NULL_RATE, UNIQUE_RATE, VALUE_RANGE, ROW_COUNT, FRESHNESS, REGEX_MATCH',
  `severity` VARCHAR(20) DEFAULT 'MEDIUM',
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `scan_scope` VARCHAR(50) DEFAULT 'LATEST_PARTITION',
  `threshold` DOUBLE COMMENT 'Failure rate threshold e.g. 0.05',
  `expected_value` VARCHAR(100),
  `min_value` DOUBLE,
  `max_value` DOUBLE,
  `regex_pattern` VARCHAR(1000),
  `action_type` VARCHAR(50) COMMENT 'ALARM, BLOCK_JOB',
  `owner` VARCHAR(100),
  `last_execution_status` VARCHAR(20),
  `last_result_value` DOUBLE,
  `last_error_message` VARCHAR(1000),
  `last_executed_at` DATETIME,
  `created_by` VARCHAR(100),
  `updated_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_quality_rule_table` (`table_id`),
  INDEX `idx_quality_rule_datasource` (`datasource_id`),
  INDEX `idx_quality_rule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Data Quality Executions
CREATE TABLE IF NOT EXISTS `dga_quality_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `table_id` BIGINT,
  `status` VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAILED, WARNING',
  `result_value` DOUBLE,
  `threshold` DOUBLE,
  `scan_scope` VARCHAR(50),
  `scan_filter` VARCHAR(1000),
  `executed_sql` VARCHAR(4000),
  `error_message` TEXT,
  `executed_by` VARCHAR(100),
  `duration_ms` BIGINT,
  `executed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_rule_exec` (`rule_id`),
  INDEX `idx_quality_exec_table` (`table_id`),
  INDEX `idx_quality_exec_status_time` (`status`, `executed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5.1 Data Quality Issues
CREATE TABLE IF NOT EXISTS `dga_quality_issue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `table_id` BIGINT,
  `issue_title` VARCHAR(255),
  `issue_description` VARCHAR(1000),
  `status` VARCHAR(20) DEFAULT 'OPEN',
  `severity` VARCHAR(20),
  `owner` VARCHAR(100),
  `result_value` DOUBLE,
  `threshold` DOUBLE,
  `last_execution_id` BIGINT,
  `first_seen_at` DATETIME,
  `last_seen_at` DATETIME,
  `resolved_at` DATETIME,
  PRIMARY KEY (`id`),
  INDEX `idx_quality_issue_rule_status` (`rule_id`, `status`),
  INDEX `idx_quality_issue_table_status` (`table_id`, `status`),
  INDEX `idx_quality_issue_owner` (`owner`)
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
  `display_name` VARCHAR(255),
  `ldap_dn` VARCHAR(500),
  `uid_number` BIGINT,
  `gid_number` BIGINT,
  `home_directory` VARCHAR(255),
  `login_shell` VARCHAR(100),
  `primary_group_name` VARCHAR(255),
  `supplementary_groups` TEXT,
  `ldap_locked` BOOLEAN DEFAULT FALSE,
  `ldap_attributes_json` LONGTEXT,
  `creation_strategy` VARCHAR(50) COMMENT 'LDAP, IPA_SSH, IPA_HTTP, SELF_REGISTER',
  `user_type` VARCHAR(30) DEFAULT 'INTERNAL' COMMENT 'INTERNAL, OUTSOURCER, TEMPORARY, SERVICE',
  `expires_at` DATETIME COMMENT 'Required for outsourcer and temporary users',
  `last_active_at` DATETIME COMMENT 'Latest activity observed from Hue/YARN/HDFS/HiveServer2/StarRocks/Doris/etc.',
  `last_active_source` VARCHAR(50) COMMENT 'LDAP, HUE, YARN, HDFS, HIVE_SERVER2, STARROCKS, DORIS, DGA',
  `cluster_name` VARCHAR(255),
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_protected` BOOLEAN DEFAULT NULL COMMENT 'NULL means use built-in protection defaults; true/false means admin override',
  `is_deleted` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_username` (`cluster_name`, `username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Data Map: Recent Views (Enhanced)
CREATE TABLE IF NOT EXISTS `dga_user_recent_views` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `view_type` VARCHAR(50) NOT NULL COMMENT 'TABLE, DATASOURCE, DATABASE, COLUMN',
  `view_content` VARCHAR(500) NOT NULL COMMENT 'e.g. db_name.table_name',
  `resource_id` BIGINT COMMENT 'Target resource id, e.g. meta_table_info.id',
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id',
  `viewed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_view` (`username`, `viewed_at` DESC),
  INDEX `idx_resource` (`resource_id`),
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

-- 12. Cluster Registry
CREATE TABLE IF NOT EXISTS `dga_cluster` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cluster_name` VARCHAR(255) NOT NULL,
  `cluster_code` VARCHAR(100) UNIQUE COMMENT 'Stable API identifier, e.g. CDH_PROD_01',
  `type` VARCHAR(50) COMMENT 'CDH, HDP, EMR, StarRocks, K8s, Other',
  `description` VARCHAR(500),
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_name` (`cluster_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Environment and cluster registry';

-- 13. Cluster Endpoints
CREATE TABLE IF NOT EXISTS `dga_cluster_endpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cluster_code` VARCHAR(100) NOT NULL,
  `endpoint_type` VARCHAR(50) NOT NULL COMMENT 'HIVE_SERVER2, HIVE_METASTORE_DB, AZKABAN_DB, DOLPHINSCHEDULER_DB, STARROCKS_JDBC, LDAP, RANGER, RANGER_DB, HDFS, YARN, HUE',
  `auth_backend` VARCHAR(50) COMMENT 'provider auth backend, e.g. SENTRY, RANGER, STARROCKS_SQL, DORIS_SQL',
  `url` VARCHAR(1000),
  `username` VARCHAR(255),
  `password` VARCHAR(255),
  `service_name` VARCHAR(255) COMMENT 'Ranger serviceName or SQL engine audit table name',
  `driver_profile` VARCHAR(50) DEFAULT 'MODERN' COMMENT 'MODERN, LEGACY_CDH5, AUTO; only for HIVE_SERVER2',
  `driver_key` VARCHAR(100) DEFAULT 'builtin-modern' COMMENT 'Selected Hive JDBC driver key for HIVE_SERVER2',
  `base_dn` VARCHAR(500),
  `user_base_dn` VARCHAR(500),
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `description` VARCHAR(500),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_cluster_endpoint` (`cluster_code`, `endpoint_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Connection endpoints for cluster resources';

-- 14. General Resource Access Records
CREATE TABLE IF NOT EXISTS `user_resource_access` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `cluster_code` VARCHAR(100),
  `cluster_name` VARCHAR(255),
  `engine_type` VARCHAR(50) COMMENT 'HIVE, STARROCKS, DORIS',
  `resource_type` VARCHAR(50) COMMENT 'provider resource type, e.g. DATABASE, TABLE, COLUMN, VIEW, FUNCTION',
  `database_name` VARCHAR(255),
  `table_name` VARCHAR(255),
  `permission` VARCHAR(50) NOT NULL,
  `grant_mode` VARCHAR(40) DEFAULT 'ROLE' COMMENT 'ROLE or DIRECT_EXCEPTION',
  `role_code` VARCHAR(128) COMMENT 'RBAC role code when grant_mode=ROLE',
  `subject_type` VARCHAR(20) COMMENT 'USER or GROUP for RBAC/direct grant subject',
  `subject_name` VARCHAR(200) COMMENT 'Actual grant subject; may differ from validation username',
  `exception_reason` VARCHAR(1000) COMMENT 'Direct grant exception reason',
  `ticket_no` VARCHAR(100) COMMENT 'Ticket number for direct exception',
  `approver` VARCHAR(100) COMMENT 'Approver for direct exception',
  `expires_at` DATETIME COMMENT 'Grant expiry time',
  `risk_level` VARCHAR(20) COMMENT 'LOW, MEDIUM, HIGH',
  `auth_backend` VARCHAR(50),
  `source` VARCHAR(50) COMMENT 'DGA_GRANT, SYNC',
  `owner` VARCHAR(100) COMMENT 'Business owner responsible for this permission',
  `collaborator_owners` VARCHAR(1000) COMMENT 'Comma-separated collaborator owners',
  `last_reviewed_at` DATETIME COMMENT 'Last high-privilege review time',
  `reviewed_by` VARCHAR(100) COMMENT 'Reviewer username',
  `review_due_at` DATETIME COMMENT 'Next review due time',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `granted_by` VARCHAR(100),
  `revoked_by` VARCHAR(100),
  `grant_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `revoke_time` DATETIME,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`id`),
  INDEX `idx_user_resource_access` (`username`, `cluster_code`, `database_name`, `table_name`, `status`),
  INDEX `idx_user_resource_subject` (`subject_type`, `subject_name`, `status`),
  INDEX `idx_user_resource_owner` (`owner`, `status`),
  INDEX `idx_user_resource_review_due` (`review_due_at`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Generalized access records across engines';

CREATE TABLE IF NOT EXISTS `auth_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(128) NOT NULL,
  `role_name` VARCHAR(200) NOT NULL,
  `cluster_name` VARCHAR(255) NOT NULL,
  `engine_type` VARCHAR(50),
  `auth_backend` VARCHAR(50),
  `owner` VARCHAR(100),
  `risk_level` VARCHAR(20) DEFAULT 'LOW',
  `expires_at` DATETIME,
  `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  `description` VARCHAR(1000),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_auth_role_code` (`role_code`),
  INDEX `idx_auth_role_cluster` (`cluster_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC role catalogue';

CREATE TABLE IF NOT EXISTS `auth_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(128) NOT NULL,
  `cluster_name` VARCHAR(255) NOT NULL,
  `resource_type` VARCHAR(50) NOT NULL,
  `database_name` VARCHAR(255),
  `table_name` VARCHAR(255),
  `permission` VARCHAR(50) NOT NULL,
  `auth_backend` VARCHAR(50),
  `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_auth_role_perm_role` (`role_code`, `status`),
  INDEX `idx_auth_role_perm_resource` (`cluster_name`, `database_name`, `table_name`, `permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC role permissions';

CREATE TABLE IF NOT EXISTS `auth_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(128) NOT NULL,
  `cluster_name` VARCHAR(255) NOT NULL,
  `subject_type` VARCHAR(20) NOT NULL,
  `subject_name` VARCHAR(200) NOT NULL,
  `auth_backend` VARCHAR(50),
  `expires_at` DATETIME,
  `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  `backend_sync_status` VARCHAR(40),
  `sync_message` VARCHAR(1000),
  `expanded_users` VARCHAR(2000),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_auth_user_role_role` (`role_code`, `status`),
  INDEX `idx_auth_user_role_subject` (`subject_type`, `subject_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC user/group role assignments';

CREATE TABLE IF NOT EXISTS `auth_role_assignment_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(128),
  `cluster_name` VARCHAR(255),
  `action` VARCHAR(80) NOT NULL,
  `subject_type` VARCHAR(20),
  `subject_name` VARCHAR(200),
  `resource_summary` VARCHAR(1000),
  `backend_status` VARCHAR(40),
  `message` VARCHAR(2000),
  `operator` VARCHAR(100),
  `action_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_auth_role_audit_role` (`role_code`, `action_time`),
  INDEX `idx_auth_role_audit_subject` (`subject_type`, `subject_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC role and assignment audit';

CREATE TABLE IF NOT EXISTS `ldap_group_empty_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cluster_code` VARCHAR(100),
  `cluster_name` VARCHAR(255) NOT NULL,
  `group_name` VARCHAR(128) NOT NULL,
  `gid_number` BIGINT,
  `empty_since` DATETIME,
  `last_seen_empty_at` DATETIME,
  `last_seen_non_empty_at` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ldap_group_empty_state` (`cluster_name`, `group_name`),
  INDEX `idx_ldap_group_empty_cluster` (`cluster_name`, `empty_since`),
  INDEX `idx_ldap_group_empty_code` (`cluster_code`, `group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LDAP 组连续空置状态快照';

-- 15. Access Governance Issues
CREATE TABLE IF NOT EXISTS `access_governance_issue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `issue_key` VARCHAR(255) NOT NULL,
  `issue_type` VARCHAR(50) NOT NULL COMMENT 'UNUSED_DATABASE_PERMISSION, HIGH_PRIVILEGE_REVIEW, UNOWNED_PERMISSION, legacy SILENT_ACCOUNT/TEMP_USER_EXPIRY_REQUIRED',
  `severity` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, CLAIMED, RESOLVED',
  `username` VARCHAR(100),
  `cluster_code` VARCHAR(100),
  `cluster_name` VARCHAR(255),
  `resource_type` VARCHAR(50),
  `database_name` VARCHAR(255),
  `table_name` VARCHAR(255),
  `permission` VARCHAR(100),
  `access_id` BIGINT,
  `owner` VARCHAR(100),
  `collaborator_owners` VARCHAR(1000),
  `assignee` VARCHAR(100),
  `source_systems` VARCHAR(255) COMMENT 'LDAP,RANGER,HIVE_SERVER2,STARROCKS,DORIS,HDFS,YARN,HUE,DGA',
  `last_active_at` DATETIME,
  `last_active_source` VARCHAR(50),
  `confidence` VARCHAR(20),
  `evidence` VARCHAR(1000),
  `recommendation` VARCHAR(1000),
  `detected_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resolved_at` DATETIME,
  `resolved_by` VARCHAR(100),
  PRIMARY KEY (`id`),
  INDEX `idx_access_issue_status` (`status`, `severity`),
  INDEX `idx_access_issue_user` (`username`, `cluster_name`),
  INDEX `idx_access_issue_key` (`issue_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Access governance risk issues';

-- 16. Access Activity Evidence
CREATE TABLE IF NOT EXISTS `access_activity_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `evidence_key` VARCHAR(255) NOT NULL,
  `username` VARCHAR(100) NOT NULL,
  `cluster_code` VARCHAR(100),
  `cluster_name` VARCHAR(255),
  `source_system` VARCHAR(50) NOT NULL COMMENT 'LDAP,RANGER,HIVE_SERVER2,STARROCKS,DORIS,HDFS,YARN,HUE,DGA',
  `last_active_at` DATETIME,
  `evidence` VARCHAR(1000),
  `confidence` VARCHAR(20) COMMENT 'HIGH, MEDIUM, LOW',
  `status` VARCHAR(20) COMMENT 'OBSERVED, PRESENT, FAILED',
  `message` VARCHAR(1000),
  `collected_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_activity_key` (`evidence_key`),
  INDEX `idx_access_activity_user` (`username`, `cluster_name`, `source_system`),
  INDEX `idx_access_activity_source` (`source_system`, `collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Activity evidence collected from access governance sources';

-- 17. Access Owner Directory
CREATE TABLE IF NOT EXISTS `access_owner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `owner_code` VARCHAR(100) NOT NULL,
  `display_name` VARCHAR(100),
  `email` VARCHAR(255),
  `source` VARCHAR(50) DEFAULT 'MANUAL' COMMENT 'MANUAL, PLATFORM_USER, LDAP',
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `created_by` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_owner_code` (`owner_code`),
  INDEX `idx_access_owner_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission owner directory';

-- 18. System and User Settings
CREATE TABLE IF NOT EXISTS `dga_system_setting` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scope` VARCHAR(100) NOT NULL COMMENT 'SYSTEM or USER:{username}',
  `setting_group` VARCHAR(100) NOT NULL COMMENT 'system, dataMap, metadataCollection, notification, personal',
  `setting_key` VARCHAR(100) NOT NULL,
  `setting_value` TEXT,
  `value_type` VARCHAR(30) DEFAULT 'STRING' COMMENT 'STRING, NUMBER, BOOLEAN, JSON',
  `updated_by` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_setting_scope_group_key` (`scope`, `setting_group`, `setting_key`),
  INDEX `idx_setting_scope_group` (`scope`, `setting_group`),
  INDEX `idx_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统与用户设置';

-- 19. Scheduler Task Owner Context
CREATE TABLE IF NOT EXISTS `dga_scheduler_task_owner` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cluster_code` VARCHAR(100) NOT NULL,
  `source_endpoint_id` BIGINT NOT NULL,
  `source_type` VARCHAR(50) NOT NULL COMMENT 'AZKABAN_DB, DOLPHINSCHEDULER_DB',
  `project_name` VARCHAR(255) NOT NULL,
  `flow_name` VARCHAR(255) NOT NULL,
  `task_name` VARCHAR(255) NOT NULL,
  `owner` VARCHAR(100),
  `collaborator_owners` VARCHAR(1000),
  `owner_source` VARCHAR(50) COMMENT 'MANUAL, AZKABAN_PROJECT',
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `remark` VARCHAR(1000),
  `updated_by` VARCHAR(100),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheduler_owner_scope` (`cluster_code`, `source_endpoint_id`, `project_name`, `flow_name`, `task_name`),
  INDEX `idx_scheduler_owner_cluster` (`cluster_code`, `source_endpoint_id`),
  INDEX `idx_scheduler_owner_owner` (`owner`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度任务责任归属与后续血缘上下文容器';

CREATE TABLE IF NOT EXISTS `dga_scheduler_task_context` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cluster_code` VARCHAR(100) NOT NULL,
  `source_type` VARCHAR(50) NOT NULL COMMENT 'AZKABAN_DB, DOLPHINSCHEDULER_DB',
  `source_endpoint_id` BIGINT NOT NULL,
  `data_source_id` BIGINT,
  `project_name` VARCHAR(255) NOT NULL,
  `flow_name` VARCHAR(255),
  `task_name` VARCHAR(255),
  `task_key` VARCHAR(500),
  `job_path` VARCHAR(1000),
  `command_text` LONGTEXT,
  `input_tables` TEXT COMMENT 'JSON array of db.table',
  `output_tables` TEXT COMMENT 'JSON array of db.table',
  `matched_table_ids` VARCHAR(2000) COMMENT 'Comma separated metadata table ids matched from input/output tables',
  `parse_status` VARCHAR(30) DEFAULT 'SUCCESS',
  `parse_message` VARCHAR(1000),
  `run_id` VARCHAR(100),
  `status` VARCHAR(20) DEFAULT 'ACTIVE',
  `parsed_at` DATETIME,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_scheduler_context_scope` (`cluster_code`, `source_endpoint_id`, `project_name`, `flow_name`),
  INDEX `idx_scheduler_context_task` (`cluster_code`, `source_endpoint_id`, `project_name`, `task_name`),
  INDEX `idx_scheduler_context_run` (`source_endpoint_id`, `data_source_id`, `run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度任务解析出的表上下文';


CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(255) NOT NULL COMMENT '加密后的密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称/显示名称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱地址',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `auth_type` varchar(20) DEFAULT 'local' COMMENT '认证方式: local, ldap, oauth2',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态: 1-启用, 0-禁用',
  `is_admin` tinyint(1) DEFAULT '0' COMMENT '是否为超级管理员',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
