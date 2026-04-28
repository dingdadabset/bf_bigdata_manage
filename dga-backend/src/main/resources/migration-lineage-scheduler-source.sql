-- Scheduler lineage source isolation migration.
-- Run once on existing MySQL environments before using AZKABAN_DB / DOLPHINSCHEDULER_DB lineage collection.

ALTER TABLE `dga_data_lineage`
  ADD COLUMN `source_type` VARCHAR(50) DEFAULT 'LEGACY' COMMENT 'AZKABAN_DB, DOLPHINSCHEDULER_DB, LEGACY' AFTER `transformation_logic`,
  ADD COLUMN `source_endpoint_id` BIGINT AFTER `source_type`,
  ADD COLUMN `data_source_id` BIGINT AFTER `source_endpoint_id`,
  ADD COLUMN `cluster_code` VARCHAR(100) AFTER `data_source_id`,
  ADD COLUMN `source_project` VARCHAR(255) AFTER `cluster_code`,
  ADD COLUMN `source_workflow` VARCHAR(255) AFTER `source_project`,
  ADD COLUMN `source_task` VARCHAR(255) AFTER `source_workflow`,
  ADD COLUMN `source_task_key` VARCHAR(255) AFTER `source_task`,
  ADD COLUMN `source_sql_hash` VARCHAR(128) AFTER `source_task_key`,
  ADD COLUMN `run_id` VARCHAR(100) AFTER `source_sql_hash`,
  ADD COLUMN `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXPIRED, DELETED' AFTER `run_id`,
  ADD COLUMN `parsed_at` DATETIME AFTER `status`;

UPDATE `dga_data_lineage`
SET `source_type` = COALESCE(`source_type`, 'LEGACY'),
    `status` = COALESCE(`status`, 'ACTIVE')
WHERE `source_type` IS NULL OR `status` IS NULL;

CREATE INDEX `idx_lineage_source_scope` ON `dga_data_lineage` (`source_endpoint_id`, `data_source_id`, `status`);
CREATE INDEX `idx_lineage_type_status` ON `dga_data_lineage` (`source_type`, `status`);

CREATE TABLE IF NOT EXISTS `dga_lineage_parse_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_type` VARCHAR(50),
  `source_endpoint_id` BIGINT,
  `source_endpoint_name` VARCHAR(500),
  `data_source_id` BIGINT,
  `data_source_name` VARCHAR(255),
  `cluster_code` VARCHAR(100),
  `run_id` VARCHAR(100),
  `trigger_type` VARCHAR(50),
  `triggered_by` VARCHAR(100),
  `status` VARCHAR(30),
  `started_at` DATETIME,
  `finished_at` DATETIME,
  `success_edge_count` INT DEFAULT 0,
  `failed_edge_count` INT DEFAULT 0,
  `message` VARCHAR(1000),
  `error_detail` TEXT,
  PRIMARY KEY (`id`),
  INDEX `idx_lineage_task_source` (`source_endpoint_id`, `data_source_id`, `started_at`),
  INDEX `idx_lineage_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度源血缘解析任务';
