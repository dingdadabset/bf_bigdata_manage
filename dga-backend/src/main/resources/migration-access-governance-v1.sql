-- Access governance risk baseline.
-- Execute manually on existing environments. Skip individual ALTER statements if the column/index already exists.

ALTER TABLE `dga_users`
  ADD COLUMN `user_type` VARCHAR(30) DEFAULT 'INTERNAL' COMMENT 'INTERNAL, OUTSOURCER, TEMPORARY, SERVICE',
  ADD COLUMN `expires_at` DATETIME NULL COMMENT 'Required for outsourcer and temporary users',
  ADD COLUMN `last_active_at` DATETIME NULL COMMENT 'Latest activity observed from Hue/YARN/HDFS/HiveServer2/StarRocks/Doris/etc.',
  ADD COLUMN `last_active_source` VARCHAR(50) NULL COMMENT 'LDAP, HUE, YARN, HDFS, HIVE_SERVER2, STARROCKS, DORIS, DGA';

ALTER TABLE `user_resource_access`
  ADD COLUMN `owner` VARCHAR(100) NULL COMMENT 'Business owner responsible for this permission',
  ADD COLUMN `collaborator_owners` VARCHAR(1000) NULL COMMENT 'Comma-separated collaborator owners',
  ADD COLUMN `last_reviewed_at` DATETIME NULL COMMENT 'Last high-privilege review time',
  ADD COLUMN `reviewed_by` VARCHAR(100) NULL COMMENT 'Reviewer username',
  ADD COLUMN `review_due_at` DATETIME NULL COMMENT 'Next review due time',
  ADD INDEX `idx_user_resource_owner` (`owner`, `status`),
  ADD INDEX `idx_user_resource_review_due` (`review_due_at`, `status`);

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
