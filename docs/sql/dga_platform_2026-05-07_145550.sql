-- MySQL dump 10.13  Distrib 8.4.0, for macos13.2 (arm64)
--
-- Host: 10.0.20.107    Database: dga_platform
-- ------------------------------------------------------
-- Server version	5.7.21-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `access_activity_evidence`
--

DROP TABLE IF EXISTS `access_activity_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_activity_evidence` (
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
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `access_governance_issue`
--

DROP TABLE IF EXISTS `access_governance_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_governance_issue` (
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
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `access_owner`
--

DROP TABLE IF EXISTS `access_owner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `access_owner` (
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_source_config`
--

DROP TABLE IF EXISTS `data_source_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_source_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `cluster_code` varchar(255) DEFAULT NULL,
  `cluster_name` varchar(255) DEFAULT NULL,
  `endpoint_id` bigint(20) DEFAULT NULL,
  `last_sync_message` varchar(1000) DEFAULT NULL,
  `last_sync_status` varchar(255) DEFAULT NULL,
  `last_sync_time` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `data_source_type`
--

DROP TABLE IF EXISTS `data_source_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data_source_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `jdbc_url_template` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_t0102m6lefauayiultj30d2kb` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_access_log`
--

DROP TABLE IF EXISTS `dga_access_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_access_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL COMMENT '操作者用户名',
  `request_type` varchar(50) NOT NULL COMMENT '请求类型：GRANT_HIVE / CREATE_USER 等',
  `target_resource` varchar(200) DEFAULT NULL COMMENT '目标资源（db/table/user 等）',
  `permission_granted` varchar(50) DEFAULT NULL COMMENT '授予/变更的权限（如 SELECT/ALL）',
  `status` varchar(20) NOT NULL COMMENT '执行状态：SUCCESS / FAILED',
  `error_message` text COMMENT '失败原因（FAILED 时记录）',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `update_time` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除：0 否 1 是',
  PRIMARY KEY (`id`),
  KEY `idx_access_log_user_time` (`username`,`created_at`),
  KEY `idx_access_log_type_status_time` (`request_type`,`status`,`created_at`),
  KEY `idx_access_log_status_time` (`status`,`created_at`),
  KEY `idx_access_log_deleted_time` (`is_deleted`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台操作审计日志（建议保留不可变更，软删除仅用于脱敏/合规）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_cluster`
--

DROP TABLE IF EXISTS `dga_cluster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_cluster` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_name` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `hive_password` varchar(255) DEFAULT NULL,
  `hive_url` varchar(255) DEFAULT NULL,
  `hive_username` varchar(255) DEFAULT NULL,
  `ldap_base` varchar(255) DEFAULT NULL,
  `ldap_password` varchar(255) DEFAULT NULL,
  `ldap_urls` varchar(255) DEFAULT NULL,
  `ldap_username` varchar(255) DEFAULT NULL,
  `ldap_user_base` varchar(255) DEFAULT NULL,
  `cluster_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_vx7b4wx153vvwmc02sth34yb` (`cluster_name`),
  UNIQUE KEY `UK_9xd1ywofdw1bww4n1hmknryc6` (`cluster_code`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_cluster_endpoint`
--

DROP TABLE IF EXISTS `dga_cluster_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_cluster_endpoint` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_backend` varchar(255) DEFAULT NULL,
  `base_dn` varchar(255) DEFAULT NULL,
  `cluster_code` varchar(255) NOT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `endpoint_type` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `service_name` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `url` varchar(1000) DEFAULT NULL,
  `user_base_dn` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `driver_profile` varchar(255) DEFAULT NULL,
  `driver_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_column_metadata`
--

DROP TABLE IF EXISTS `dga_column_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_column_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) NOT NULL,
  `column_type` varchar(255) NOT NULL,
  `comment_str` varchar(255) DEFAULT NULL,
  `is_primary_key` bit(1) DEFAULT NULL,
  `security_level` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_data_lineage`
--

DROP TABLE IF EXISTS `dga_data_lineage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_data_lineage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `lineage_type` varchar(255) NOT NULL,
  `source_table_id` bigint(20) NOT NULL,
  `target_table_id` bigint(20) NOT NULL,
  `transformation_logic` varchar(255) DEFAULT NULL,
  `cluster_code` varchar(255) DEFAULT NULL,
  `data_source_id` bigint(20) DEFAULT NULL,
  `parsed_at` datetime(6) DEFAULT NULL,
  `run_id` varchar(255) DEFAULT NULL,
  `source_endpoint_id` bigint(20) DEFAULT NULL,
  `source_project` varchar(255) DEFAULT NULL,
  `source_sql_hash` varchar(255) DEFAULT NULL,
  `source_task` varchar(255) DEFAULT NULL,
  `source_task_key` varchar(255) DEFAULT NULL,
  `source_type` varchar(255) DEFAULT NULL,
  `source_workflow` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_data_theme`
--

DROP TABLE IF EXISTS `dga_data_theme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_data_theme` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `description` text,
  `parent_id` bigint(20) DEFAULT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `theme_name` varchar(255) NOT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_governance_task`
--

DROP TABLE IF EXISTS `dga_governance_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_governance_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime(6) DEFAULT NULL,
  `handler` varchar(255) DEFAULT NULL,
  `issue_description` varchar(255) DEFAULT NULL,
  `issue_type` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL,
  `task_status` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=247 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_lineage_parse_task`
--

DROP TABLE IF EXISTS `dga_lineage_parse_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_lineage_parse_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) DEFAULT NULL,
  `data_source_id` bigint(20) DEFAULT NULL,
  `data_source_name` varchar(255) DEFAULT NULL,
  `error_detail` text,
  `failed_edge_count` int(11) DEFAULT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `run_id` varchar(255) DEFAULT NULL,
  `source_endpoint_id` bigint(20) DEFAULT NULL,
  `source_endpoint_name` varchar(255) DEFAULT NULL,
  `source_type` varchar(255) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `success_edge_count` int(11) DEFAULT NULL,
  `trigger_type` varchar(255) DEFAULT NULL,
  `triggered_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_metadata_collection_task`
--

DROP TABLE IF EXISTS `dga_metadata_collection_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_metadata_collection_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) DEFAULT NULL,
  `datasource_id` bigint(20) NOT NULL,
  `datasource_name` varchar(255) DEFAULT NULL,
  `error_detail` text,
  `failed_table_count` int(11) DEFAULT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `success_table_count` int(11) DEFAULT NULL,
  `trigger_type` varchar(255) DEFAULT NULL,
  `triggered_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_metadata_tag`
--

DROP TABLE IF EXISTS `dga_metadata_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_metadata_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `color` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `tag_name` varchar(255) NOT NULL,
  `tag_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_metric_definition`
--

DROP TABLE IF EXISTS `dga_metric_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_metric_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_definition` text,
  `calculation_logic` text,
  `create_time` datetime(6) DEFAULT NULL,
  `metric_code` varchar(255) NOT NULL,
  `metric_name` varchar(255) NOT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_quality_execution`
--

DROP TABLE IF EXISTS `dga_quality_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_quality_execution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `error_message` varchar(255) DEFAULT NULL,
  `executed_at` datetime(6) DEFAULT NULL,
  `result_value` double DEFAULT NULL,
  `rule_id` bigint(20) NOT NULL,
  `status` varchar(255) NOT NULL,
  `duration_ms` bigint(20) DEFAULT NULL,
  `executed_by` varchar(255) DEFAULT NULL,
  `executed_sql` varchar(4000) DEFAULT NULL,
  `scan_filter` varchar(1000) DEFAULT NULL,
  `scan_scope` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) DEFAULT NULL,
  `threshold` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_quality_issue`
--

DROP TABLE IF EXISTS `dga_quality_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_quality_issue` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_quality_rule`
--

DROP TABLE IF EXISTS `dga_quality_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_quality_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `action_type` varchar(255) DEFAULT NULL,
  `column_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `rule_type` varchar(255) NOT NULL,
  `table_id` bigint(20) NOT NULL,
  `threshold` double DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `datasource_id` bigint(20) DEFAULT NULL,
  `db_name` varchar(255) DEFAULT NULL,
  `expected_value` varchar(255) DEFAULT NULL,
  `last_error_message` varchar(1000) DEFAULT NULL,
  `last_executed_at` datetime(6) DEFAULT NULL,
  `last_execution_status` varchar(255) DEFAULT NULL,
  `last_result_value` double DEFAULT NULL,
  `max_value` double DEFAULT NULL,
  `min_value` double DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `regex_pattern` varchar(255) DEFAULT NULL,
  `rule_name` varchar(255) DEFAULT NULL,
  `scan_scope` varchar(255) DEFAULT NULL,
  `severity` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_resource_link`
--

DROP TABLE IF EXISTS `dga_resource_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_resource_link` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `url` varchar(500) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `env` varchar(20) DEFAULT NULL,
  `logo_url` varchar(500) DEFAULT NULL,
  `recommended` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(20) DEFAULT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_url` (`url`),
  KEY `idx_category_env` (`category`,`env`),
  KEY `idx_recommended` (`recommended`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COMMENT='资源管理-链接配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_system_setting`
--

DROP TABLE IF EXISTS `dga_system_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_system_setting` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_table_business_metadata`
--

DROP TABLE IF EXISTS `dga_table_business_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_table_business_metadata` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_definition` text,
  `business_description` text,
  `business_owner` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `theme_id` bigint(20) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_table_metadata`
--

DROP TABLE IF EXISTS `dga_table_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_table_metadata` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_table_score`
--

DROP TABLE IF EXISTS `dga_table_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_table_score` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `governance_advice` varchar(255) DEFAULT NULL,
  `report_date` date NOT NULL,
  `score_cost` decimal(19,2) DEFAULT NULL,
  `score_quality` decimal(19,2) DEFAULT NULL,
  `score_security` decimal(19,2) DEFAULT NULL,
  `score_spec` decimal(19,2) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `total_score` decimal(19,2) DEFAULT NULL,
  `cost_score` decimal(5,2) DEFAULT NULL,
  `quality_score` decimal(5,2) DEFAULT NULL,
  `security_score` decimal(5,2) DEFAULT NULL,
  `storage_score` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_table_tag_mapping`
--

DROP TABLE IF EXISTS `dga_table_tag_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_table_tag_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) DEFAULT NULL,
  `assigned_by` varchar(255) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_user_recent_views`
--

DROP TABLE IF EXISTS `dga_user_recent_views`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_user_recent_views` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `view_content` varchar(255) NOT NULL,
  `view_type` varchar(255) NOT NULL,
  `viewed_at` datetime(6) DEFAULT NULL,
  `datasource_id` bigint(20) DEFAULT NULL,
  `resource_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dga_users`
--

DROP TABLE IF EXISTS `dga_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dga_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `username` varchar(100) NOT NULL COMMENT '用户名（唯一）',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `first_name` varchar(100) DEFAULT NULL COMMENT '名',
  `last_name` varchar(100) DEFAULT NULL COMMENT '姓',
  `creation_strategy` varchar(50) DEFAULT NULL COMMENT '创建来源策略：LDAP, IPA_SSH, IPA_HTTP',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `password` varchar(255) DEFAULT NULL COMMENT '密码（如不存储可为 NULL）',
  `is_deleted` int(11) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0=未删除，1=已删除',
  `update_time` datetime(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间（微秒）',
  `cluster_name` varchar(255) NOT NULL DEFAULT 'CDH' COMMENT '集群名称',
  `is_protected` bit(1) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `last_active_at` datetime(6) DEFAULT NULL,
  `last_active_source` varchar(255) DEFAULT NULL,
  `user_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4no4piterbhq6h4hjmqnp56e1` (`username`,`cluster_name`),
  UNIQUE KEY `uk_cluster_username` (`cluster_name`,`username`)
) ENGINE=InnoDB AUTO_INCREMENT=182 DEFAULT CHARSET=utf8mb4 COMMENT='DGA 用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_change_log`
--

DROP TABLE IF EXISTS `meta_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_change_log` (
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
  KEY `idx_discover_time` (`discover_time`) COMMENT '按发现时间查询',
  KEY `idx_object_name` (`object_name`) COMMENT '按对象名查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据变更审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_cluster_info`
--

DROP TABLE IF EXISTS `meta_cluster_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_cluster_info` (
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
  UNIQUE KEY `uk_cluster_code` (`cluster_code`) COMMENT '集群编码唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='集群注册信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_column_info`
--

DROP TABLE IF EXISTS `meta_column_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_column_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) NOT NULL,
  `column_type` varchar(255) DEFAULT NULL,
  `column_comment` varchar(255) DEFAULT NULL,
  `is_partition_key` bit(1) DEFAULT NULL,
  `sort_order` int(11) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `comment_str` varchar(255) DEFAULT NULL,
  `is_primary_key` bit(1) DEFAULT NULL,
  `security_level` varchar(255) DEFAULT NULL,
  `data_type` varchar(255) NOT NULL,
  `comment` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25024 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_column_std`
--

DROP TABLE IF EXISTS `meta_column_std`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_column_std` (
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
  UNIQUE KEY `uk_table_col` (`table_id`,`column_name`) COMMENT '表内字段名唯一',
  KEY `idx_table_id` (`table_id`) COMMENT '关联表查询索引',
  KEY `idx_col_name` (`column_name`) COMMENT '全局字段名搜索索引 (用于血缘分析)'
) ENGINE=InnoDB AUTO_INCREMENT=4197 DEFAULT CHARSET=utf8mb4 COMMENT='标准化字段信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_database_std`
--

DROP TABLE IF EXISTS `meta_database_std`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_database_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID (meta_cluster_info.id)',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '数据库所有者',
  `owner_type` varchar(32) DEFAULT 'USER' COMMENT '所有者类型: USER, ROLE',
  `description` text COMMENT '数据库注释/描述',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT 'HDFS 存储根路径',
  `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (DB_PROPERTIES)，存储源端特有属性',
  `source_db_id` varchar(128) DEFAULT NULL COMMENT '源端数据库 ID (用于增量比对)',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地更新时间',
  `connection_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_db` (`cluster_id`,`db_name`) COMMENT '集群内数据库名唯一',
  KEY `idx_db_name` (`db_name`) COMMENT '数据库名查询索引'
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COMMENT='标准化数据库信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_enhanced_table`
--

DROP TABLE IF EXISTS `meta_enhanced_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_enhanced_table` (
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
) ENGINE=InnoDB AUTO_INCREMENT=2198 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_partition_info`
--

DROP TABLE IF EXISTS `meta_partition_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_partition_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cluster_code` varchar(255) DEFAULT NULL,
  `datasource_id` bigint(20) DEFAULT NULL,
  `db_name` varchar(255) DEFAULT NULL,
  `last_access_time` datetime(6) DEFAULT NULL,
  `last_modify_time` datetime(6) DEFAULT NULL,
  `hdfs_path` varchar(1000) DEFAULT NULL,
  `partition_name` varchar(500) NOT NULL,
  `partition_spec` varchar(1000) DEFAULT NULL,
  `record_count` bigint(20) DEFAULT NULL,
  `storage_format` varchar(255) DEFAULT NULL,
  `sync_time` datetime(6) DEFAULT NULL,
  `table_id` bigint(20) NOT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `table_size` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=181522 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_partition_std`
--

DROP TABLE IF EXISTS `meta_partition_std`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_partition_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_id` bigint(20) NOT NULL COMMENT '关联表 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称',
  `table_name` varchar(128) NOT NULL COMMENT '表名称',
  `partition_name` varchar(256) NOT NULL COMMENT '分区名称 (如: dt=20231001/region=cn)',
  `partition_values` json DEFAULT NULL COMMENT '分区键值对解析 ({ "dt": "20231001", "region": "cn" })',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT '分区具体存储路径',
  `create_time` datetime DEFAULT NULL COMMENT '分区创建时间',
  `last_access_time` datetime DEFAULT NULL COMMENT '最后访问时间',
  `parameters_json` json DEFAULT NULL COMMENT '分区扩展参数',
  `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '分区数据大小',
  `row_count` bigint(20) DEFAULT '0' COMMENT '分区行数',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_part` (`table_id`,`partition_name`) COMMENT '表内分区唯一',
  KEY `idx_table_id` (`table_id`) COMMENT '关联表查询',
  KEY `idx_create_time` (`create_time`) COMMENT '按分区时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化分区信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_table_info`
--

DROP TABLE IF EXISTS `meta_table_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_table_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `datasource_id` bigint(20) DEFAULT NULL,
  `db_name` varchar(255) NOT NULL,
  `file_count` bigint(20) DEFAULT NULL,
  `last_access_time` datetime(6) DEFAULT NULL,
  `last_modify_time` datetime(6) DEFAULT NULL,
  `hdfs_path` varchar(255) DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `partition_count` int(11) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `storage_format` varchar(255) DEFAULT NULL,
  `table_comment` varchar(255) DEFAULT NULL,
  `table_name` varchar(255) NOT NULL,
  `table_type` varchar(255) DEFAULT NULL,
  `table_size` bigint(20) DEFAULT NULL,
  `sync_time` datetime(6) DEFAULT NULL,
  `datasource_name` varchar(255) DEFAULT NULL,
  `create_time` datetime(6) DEFAULT NULL,
  `dw_level` varchar(255) DEFAULT NULL,
  `governance_score` decimal(19,2) DEFAULT NULL,
  `is_partitioned` bit(1) DEFAULT NULL,
  `lifecycle_days` int(11) DEFAULT NULL,
  `record_count` bigint(20) DEFAULT NULL,
  `location_path` varchar(255) DEFAULT NULL,
  `total_size` bigint(20) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `cluster_code` varchar(255) DEFAULT NULL,
  `owner_source` varchar(255) DEFAULT NULL,
  `source_owner` varchar(255) DEFAULT NULL,
  `lifecycle_status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1163 DEFAULT CHARSET=utf8mb4 COMMENT='使用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `meta_table_std`
--

DROP TABLE IF EXISTS `meta_table_std`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meta_table_std` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `cluster_id` bigint(20) NOT NULL COMMENT '关联集群 ID',
  `db_name` varchar(128) NOT NULL COMMENT '数据库名称 (冗余字段，方便查询)',
  `table_name` varchar(128) NOT NULL COMMENT '表名称',
  `table_type` varchar(32) NOT NULL COMMENT '表类型: MANAGED_TABLE, EXTERNAL_TABLE, VIRTUAL_VIEW, MATERIALIZED_VIEW',
  `format_type` varchar(64) DEFAULT NULL COMMENT '文件格式: ORC, PARQUET, AVRO, TEXTFILE, SEQUENCEFILE',
  `input_format` varchar(256) DEFAULT NULL COMMENT 'InputFormat 类名',
  `output_format` varchar(256) DEFAULT NULL COMMENT 'OutputFormat 类名',
  `serde_class` varchar(256) DEFAULT NULL COMMENT 'SerDe 序列化类名',
  `location_uri` varchar(1024) DEFAULT NULL COMMENT '数据存储路径 (HDFS/S3/OSS)',
  `create_time` datetime DEFAULT NULL COMMENT '表创建时间 (源端)',
  `last_ddl_time` datetime DEFAULT NULL COMMENT '最后 DDL 变更时间 (源端)',
  `retention` int(11) DEFAULT '0' COMMENT '数据保留时间 (天)',
  `view_expanded_text` longtext COMMENT '视图展开后的 SQL (如果是视图)',
  `view_original_text` longtext COMMENT '视图原始创建 SQL',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '表所有者',
  `description` text COMMENT '表注释/描述',
  `is_partitioned` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否分区表: 0-否, 1-是',
  `partition_keys` json DEFAULT NULL COMMENT '分区字段列表 (简略缓存，如 ["dt", "region"])',
  `parameters_json` json DEFAULT NULL COMMENT '扩展参数 (TBLPROPERTIES)，兼容各版本差异',
  `source_tbl_id` varchar(128) DEFAULT NULL COMMENT '源端表 ID (用于增量比对)',
  `data_size_bytes` bigint(20) DEFAULT '0' COMMENT '数据总大小 (字节)，需定期统计更新',
  `row_count` bigint(20) DEFAULT '0' COMMENT '预估行数，需定期统计更新',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 0-否, 1-是',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据同步时间',
  `create_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  `update_time_local` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地记录更新时间',
  `partition_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cluster_db_tbl` (`cluster_id`,`db_name`,`table_name`) COMMENT '集群内表名唯一',
  KEY `idx_db_table` (`db_name`,`table_name`) COMMENT '通用查询索引',
  KEY `idx_format_type` (`format_type`) COMMENT '按文件格式统计索引',
  KEY `idx_last_ddl` (`last_ddl_time`) COMMENT '按变更时间排序索引'
) ENGINE=InnoDB AUTO_INCREMENT=362 DEFAULT CHARSET=utf8mb4 COMMENT='标准化表信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_hive_access`
--

DROP TABLE IF EXISTS `user_hive_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_hive_access` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `cluster_name` varchar(255) NOT NULL COMMENT '集群名称',
  `database_name` varchar(255) NOT NULL COMMENT 'Hive 库名',
  `grant_time` datetime(6) DEFAULT NULL COMMENT '授权时间（微秒）',
  `granted_by` varchar(255) DEFAULT NULL COMMENT '授权人',
  `permission` varchar(255) NOT NULL COMMENT '权限（如 SELECT/INSERT/ALL 等，按业务约定）',
  `status` varchar(255) NOT NULL COMMENT '状态（如 ACTIVE/REVOKED/PENDING 等，按业务约定）',
  `table_name` varchar(255) DEFAULT NULL COMMENT 'Hive 表名；NULL 表示库级权限',
  `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间（微秒）',
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0=未删除，1=已删除',
  `revoke_time` datetime(6) DEFAULT NULL COMMENT '回收权限时间（微秒）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=233 DEFAULT CHARSET=utf8mb4 COMMENT='用户 Hive 权限记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_resource_access`
--

DROP TABLE IF EXISTS `user_resource_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_resource_access` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auth_backend` varchar(255) DEFAULT NULL,
  `cluster_code` varchar(255) DEFAULT NULL,
  `cluster_name` varchar(255) DEFAULT NULL,
  `database_name` varchar(255) DEFAULT NULL,
  `engine_type` varchar(255) DEFAULT NULL,
  `grant_time` datetime(6) DEFAULT NULL,
  `granted_by` varchar(255) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `permission` varchar(255) NOT NULL,
  `resource_type` varchar(255) DEFAULT NULL,
  `revoke_time` datetime(6) DEFAULT NULL,
  `source` varchar(255) DEFAULT NULL,
  `status` varchar(255) NOT NULL,
  `table_name` varchar(255) DEFAULT NULL,
  `update_time` datetime(6) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `last_reviewed_at` datetime(6) DEFAULT NULL,
  `owner` varchar(255) DEFAULT NULL,
  `review_due_at` datetime(6) DEFAULT NULL,
  `reviewed_by` varchar(255) DEFAULT NULL,
  `revoked_by` varchar(255) DEFAULT NULL,
  `collaborator_owners` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `mobile` varchar(20) DEFAULT NULL,
  `open_id` varchar(100) DEFAULT NULL,
  `provider` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'dga_platform'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-07 14:55:53
