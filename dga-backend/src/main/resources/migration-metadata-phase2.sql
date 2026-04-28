-- Metadata phase 2 manual migration.
-- Run once on existing MySQL environments before enabling the new metadata management UI.

ALTER TABLE `meta_table_info`
  ADD COLUMN `partition_count` BIGINT DEFAULT 0 AFTER `record_count`,
  ADD COLUMN `lifecycle_status` VARCHAR(30) DEFAULT 'ONLINE' COMMENT 'ONLINE, DEPRECATED, OFFLINE' AFTER `partition_count`;

CREATE TABLE IF NOT EXISTS `meta_partition_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL,
  `datasource_id` BIGINT,
  `cluster_code` VARCHAR(100),
  `db_name` VARCHAR(100),
  `table_name` VARCHAR(100),
  `partition_name` VARCHAR(500) NOT NULL,
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
  INDEX `idx_metric_status` (`status`)
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
