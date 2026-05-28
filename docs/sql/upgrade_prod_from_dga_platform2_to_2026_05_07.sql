-- DGA 生产库结构升级脚本
-- 从: docs/sql/dga_platform2.sql（生产旧版本）
-- 到: docs/sql/dga_platform_2026-05-07_145550.sql（最新版本）
-- 生成时间: 2026-05-22 09:53:24
--
-- 执行前务必备份生产库：
--   mysqldump -h <host> -u <user> -p --single-transaction --routines --triggers dga_platform > dga_platform_backup_$(date +%F_%H%M%S).sql
--
-- 说明：
-- 1. 本脚本只做结构升级，不插入/覆盖业务数据。
-- 2. 本脚本不执行 DROP COLUMN / DROP TABLE，避免误删生产数据。
-- 3. AUTO_INCREMENT、ROW_FORMAT、dump 中显式字符集写法差异已忽略。
-- 4. 如遇 Duplicate column/key，说明该变更已执行，可跳过对应语句。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 升级表 `access_activity_evidence`
ALTER TABLE `access_activity_evidence`
  ADD KEY `idx_access_activity_user` (`username`,`cluster_name`,`source_system`),
  ADD KEY `idx_access_activity_source` (`source_system`,`collected_at`),
  ADD KEY `idx_access_activity_key` (`evidence_key`);

-- 升级表 `access_governance_issue`
ALTER TABLE `access_governance_issue`
  ADD KEY `idx_access_issue_status` (`status`,`severity`),
  ADD KEY `idx_access_issue_user` (`username`,`cluster_name`),
  ADD KEY `idx_access_issue_key` (`issue_key`);

-- 升级表 `access_owner`
ALTER TABLE `access_owner`
  ADD KEY `idx_access_owner_code` (`owner_code`),
  ADD KEY `idx_access_owner_status` (`status`);

-- 升级表 `data_source_type`
ALTER TABLE `data_source_type`
  ADD UNIQUE KEY `UK_t0102m6lefauayiultj30d2kb` (`code`);

-- 升级表 `dga_access_log`
ALTER TABLE `dga_access_log`
  MODIFY COLUMN `username` varchar(100) NOT NULL COMMENT '操作者用户名',
  MODIFY COLUMN `request_type` varchar(50) NOT NULL COMMENT '请求类型：GRANT_HIVE / CREATE_USER 等',
  MODIFY COLUMN `target_resource` varchar(200) DEFAULT NULL COMMENT '目标资源（db/table/user 等）',
  MODIFY COLUMN `permission_granted` varchar(50) DEFAULT NULL COMMENT '授予/变更的权限（如 SELECT/ALL）',
  MODIFY COLUMN `status` varchar(20) NOT NULL COMMENT '执行状态：SUCCESS / FAILED',
  MODIFY COLUMN `error_message` text COMMENT '失败原因（FAILED 时记录）',
  MODIFY COLUMN `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  MODIFY COLUMN `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  MODIFY COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除：0 否 1 是',
  ADD KEY `idx_access_log_user_time` (`username`,`created_at`),
  ADD KEY `idx_access_log_type_status_time` (`request_type`,`status`,`created_at`),
  ADD KEY `idx_access_log_status_time` (`status`,`created_at`),
  ADD KEY `idx_access_log_deleted_time` (`is_deleted`,`created_at`);

-- 升级表 `dga_cluster`
ALTER TABLE `dga_cluster`
  ADD UNIQUE KEY `UK_vx7b4wx153vvwmc02sth34yb` (`cluster_name`),
  ADD UNIQUE KEY `UK_9xd1ywofdw1bww4n1hmknryc6` (`cluster_code`);

-- 升级表 `dga_data_theme`
ALTER TABLE `dga_data_theme`
  MODIFY COLUMN `description` text;

-- 升级表 `dga_lineage_parse_task`
ALTER TABLE `dga_lineage_parse_task`
  MODIFY COLUMN `error_detail` text;

-- 升级表 `dga_metadata_collection_task`
ALTER TABLE `dga_metadata_collection_task`
  MODIFY COLUMN `error_detail` text;

-- 升级表 `dga_metric_definition`
ALTER TABLE `dga_metric_definition`
  MODIFY COLUMN `business_definition` text,
  MODIFY COLUMN `calculation_logic` text;

-- 升级表 `dga_resource_link`
ALTER TABLE `dga_resource_link`
  MODIFY COLUMN `url` varchar(500) NOT NULL,
  MODIFY COLUMN `description` varchar(500) DEFAULT NULL,
  MODIFY COLUMN `category` varchar(50) DEFAULT NULL,
  MODIFY COLUMN `env` varchar(20) DEFAULT NULL,
  MODIFY COLUMN `logo_url` varchar(500) DEFAULT NULL,
  MODIFY COLUMN `recommended` tinyint(1) NOT NULL DEFAULT '0',
  MODIFY COLUMN `status` varchar(20) DEFAULT NULL,
  MODIFY COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  MODIFY COLUMN `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  MODIFY COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD UNIQUE KEY `uk_resource_url` (`url`),
  ADD KEY `idx_category_env` (`category`,`env`),
  ADD KEY `idx_recommended` (`recommended`);

-- 升级表 `dga_system_setting`
ALTER TABLE `dga_system_setting`
  MODIFY COLUMN `setting_value` longtext,
  ADD UNIQUE KEY `uk_setting_scope_group_key` (`scope`,`setting_group`,`setting_key`),
  ADD KEY `idx_setting_scope_group` (`scope`,`setting_group`),
  ADD KEY `idx_setting_key` (`setting_key`);

-- 升级表 `dga_table_business_metadata`
ALTER TABLE `dga_table_business_metadata`
  MODIFY COLUMN `business_definition` text,
  MODIFY COLUMN `business_description` text;

-- 升级表 `dga_table_score`
ALTER TABLE `dga_table_score`
  MODIFY COLUMN `total_score` decimal(19,2) DEFAULT NULL;

-- 升级表 `dga_users`
ALTER TABLE `dga_users`
  MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  MODIFY COLUMN `username` varchar(100) NOT NULL COMMENT '用户名（唯一）',
  MODIFY COLUMN `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  MODIFY COLUMN `first_name` varchar(100) DEFAULT NULL COMMENT '名',
  MODIFY COLUMN `last_name` varchar(100) DEFAULT NULL COMMENT '姓',
  MODIFY COLUMN `creation_strategy` varchar(50) DEFAULT NULL COMMENT '创建来源策略：LDAP, IPA_SSH, IPA_HTTP',
  MODIFY COLUMN `create_time` datetime NOT NULL COMMENT '创建时间',
  MODIFY COLUMN `password` varchar(255) DEFAULT NULL COMMENT '密码（如不存储可为 NULL）',
  MODIFY COLUMN `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0=未删除，1=已删除',
  MODIFY COLUMN `update_time` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间（微秒）',
  MODIFY COLUMN `cluster_name` varchar(255) NOT NULL DEFAULT 'CDH' COMMENT '集群名称',
  ADD UNIQUE KEY `UK4no4piterbhq6h4hjmqnp56e1` (`username`,`cluster_name`),
  ADD UNIQUE KEY `uk_cluster_username` (`cluster_name`,`username`);

-- 升级表 `meta_change_log`
ALTER TABLE `meta_change_log`
  MODIFY COLUMN `old_value` text COMMENT '变更前内容 (JSON 格式)',
  MODIFY COLUMN `new_value` text COMMENT '变更后内容 (JSON 格式)',
  MODIFY COLUMN `operator` varchar(64) DEFAULT 'SYSTEM_SYNC' COMMENT '操作人/触发源',
  MODIFY COLUMN `is_processed` tinyint(4) DEFAULT '0' COMMENT '是否已处理 (用于触发下游任务): 0-否, 1-是',
  ADD KEY `idx_discover_time` (`discover_time`) COMMENT '按发现时间查询',
  ADD KEY `idx_object_name` (`object_name`) COMMENT '按对象名查询';

-- 升级表 `meta_cluster_info`
ALTER TABLE `meta_cluster_info`
  MODIFY COLUMN `sync_strategy` varchar(32) DEFAULT 'API' COMMENT '同步策略: API (推荐), DIRECT_DB',
  MODIFY COLUMN `created_by` varchar(64) DEFAULT 'SYSTEM' COMMENT '创建人',
  ADD UNIQUE KEY `uk_cluster_code` (`cluster_code`) COMMENT '集群编码唯一索引';

-- 升级表 `meta_column_info`
ALTER TABLE `meta_column_info`
  MODIFY COLUMN `column_type` varchar(255) DEFAULT NULL,
  MODIFY COLUMN `data_type` varchar(255) NOT NULL;

-- 升级表 `meta_column_std`
ALTER TABLE `meta_column_std`
  MODIFY COLUMN `is_nullable` tinyint(4) DEFAULT '1' COMMENT '是否可空: 0-否, 1-是 (高版本 Hive 支持)',
  ADD UNIQUE KEY `uk_table_col` (`table_id`,`column_name`) COMMENT '表内字段名唯一',
  ADD KEY `idx_table_id` (`table_id`) COMMENT '关联表查询索引',
  ADD KEY `idx_col_name` (`column_name`) COMMENT '全局字段名搜索索引 (用于血缘分析)';

-- 升级表 `meta_database_std`
ALTER TABLE `meta_database_std`
  MODIFY COLUMN `owner_type` varchar(32) DEFAULT 'USER' COMMENT '所有者类型: USER, ROLE',
  MODIFY COLUMN `description` text COMMENT '数据库注释/描述',
  MODIFY COLUMN `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (DB_PROPERTIES)，存储源端特有属性',
  MODIFY COLUMN `source_db_id` varchar(128) DEFAULT NULL COMMENT '源端数据库 ID (用于增量比对)',
  ADD UNIQUE KEY `uk_cluster_db` (`cluster_id`,`db_name`) COMMENT '集群内数据库名唯一',
  ADD KEY `idx_db_name` (`db_name`) COMMENT '数据库名查询索引';

-- 升级表 `meta_enhanced_table`
ALTER TABLE `meta_enhanced_table`
  MODIFY COLUMN `description` text,
  MODIFY COLUMN `partition_keys` text;

-- 升级表 `meta_partition_std`
ALTER TABLE `meta_partition_std`
  MODIFY COLUMN `partition_values` json DEFAULT NULL COMMENT '分区键值对解析 ({ "dt": "20231001", "region": "cn" })',
  MODIFY COLUMN `location_uri` varchar(1024) DEFAULT NULL COMMENT '分区具体存储路径',
  MODIFY COLUMN `parameters_json` json DEFAULT NULL COMMENT '分区扩展参数',
  MODIFY COLUMN `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '分区数据大小',
  MODIFY COLUMN `row_count` bigint(20) DEFAULT '0' COMMENT '分区行数',
  ADD UNIQUE KEY `uk_table_part` (`table_id`,`partition_name`) COMMENT '表内分区唯一',
  ADD KEY `idx_table_id` (`table_id`) COMMENT '关联表查询',
  ADD KEY `idx_create_time` (`create_time`) COMMENT '按分区时间查询';

-- 升级表 `meta_table_info`
ALTER TABLE `meta_table_info`
  MODIFY COLUMN `datasource_id` bigint(20) DEFAULT NULL,
  MODIFY COLUMN `partition_count` int(11) DEFAULT NULL,
  MODIFY COLUMN `table_comment` varchar(255) DEFAULT NULL;

-- 升级表 `meta_table_std`
ALTER TABLE `meta_table_std`
  MODIFY COLUMN `db_name` varchar(128) NOT NULL COMMENT '数据库名称 (冗余字段，方便查询)',
  MODIFY COLUMN `location_uri` varchar(1024) DEFAULT NULL COMMENT '数据存储路径 (HDFS/S3/OSS)',
  MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '表创建时间 (源端)',
  MODIFY COLUMN `last_ddl_time` datetime DEFAULT NULL COMMENT '最后 DDL 变更时间 (源端)',
  MODIFY COLUMN `retention` int(11) DEFAULT '0' COMMENT '数据保留时间 (天)',
  MODIFY COLUMN `view_expanded_text` longtext COMMENT '视图展开后的 SQL (如果是视图)',
  MODIFY COLUMN `view_original_text` longtext COMMENT '视图原始创建 SQL',
  MODIFY COLUMN `description` text COMMENT '表注释/描述',
  MODIFY COLUMN `partition_keys` json DEFAULT NULL COMMENT '分区字段列表 (简略缓存，如 ["dt", "region"])',
  MODIFY COLUMN `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (TBLPROPERTIES)，兼容各版本差异',
  MODIFY COLUMN `source_tbl_id` varchar(128) DEFAULT NULL COMMENT '源端表 ID (用于增量比对)',
  MODIFY COLUMN `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '数据总大小 (字节)，需定期统计更新',
  MODIFY COLUMN `row_count` bigint(20) DEFAULT '0' COMMENT '预估行数，需定期统计更新',
  ADD UNIQUE KEY `uk_cluster_db_tbl` (`cluster_id`,`db_name`,`table_name`) COMMENT '集群内表名唯一',
  ADD KEY `idx_db_table` (`db_name`,`table_name`) COMMENT '通用查询索引',
  ADD KEY `idx_format_type` (`format_type`) COMMENT '按文件格式统计索引',
  ADD KEY `idx_last_ddl` (`last_ddl_time`) COMMENT '按变更时间排序索引';

-- 升级表 `user_hive_access`
ALTER TABLE `user_hive_access`
  MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  MODIFY COLUMN `cluster_name` varchar(255) NOT NULL COMMENT '集群名称',
  MODIFY COLUMN `database_name` varchar(255) NOT NULL COMMENT 'Hive 库名',
  MODIFY COLUMN `grant_time` datetime(6) DEFAULT NULL COMMENT '授权时间（微秒）',
  MODIFY COLUMN `granted_by` varchar(255) DEFAULT NULL COMMENT '授权人',
  MODIFY COLUMN `permission` varchar(255) NOT NULL COMMENT '权限（如 SELECT/INSERT/ALL 等，按业务约定）',
  MODIFY COLUMN `status` varchar(255) NOT NULL COMMENT '状态（如 ACTIVE/REVOKED/PENDING 等，按业务约定）',
  MODIFY COLUMN `table_name` varchar(255) DEFAULT NULL COMMENT 'Hive 表名；NULL 表示库级权限',
  MODIFY COLUMN `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间（微秒）',
  MODIFY COLUMN `username` varchar(255) NOT NULL COMMENT '用户名',
  MODIFY COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0=未删除，1=已删除',
  MODIFY COLUMN `revoke_time` datetime(6) DEFAULT NULL COMMENT '回收权限时间（微秒）';

-- 升级表 `user_resource_access`
ALTER TABLE `user_resource_access`
  MODIFY COLUMN `is_deleted` tinyint(1) DEFAULT '0';

-- 升级表 `users`
ALTER TABLE `users`
  MODIFY COLUMN `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `username` varchar(50) NOT NULL COMMENT '登录账号',
  MODIFY COLUMN `password` varchar(255) NOT NULL COMMENT '加密后的密码',
  MODIFY COLUMN `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称/显示名称',
  MODIFY COLUMN `email` varchar(100) DEFAULT NULL COMMENT '邮箱地址',
  MODIFY COLUMN `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  MODIFY COLUMN `auth_type` varchar(20) DEFAULT 'local' COMMENT '认证方式: local, ldap, oauth2',
  MODIFY COLUMN `status` tinyint(1) DEFAULT '1' COMMENT '状态: 1-启用, 0-禁用',
  MODIFY COLUMN `is_admin` tinyint(1) DEFAULT '0' COMMENT '是否为超级管理员',
  MODIFY COLUMN `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  MODIFY COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  ADD UNIQUE KEY `idx_username` (`username`);

SET FOREIGN_KEY_CHECKS = 1;

-- 升级后建议检查：
--   SHOW WARNINGS;
--   SELECT COUNT(*) FROM access_governance_issue;
--   SELECT COUNT(*) FROM user_resource_access;

-- 变更摘要：
--   ADD_TABLE: 0
--   ADD_COL: 0
--   MOD_COL: 100
--   ADD_KEY: 39
--   MOD_KEY: 0