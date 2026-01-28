-- Data Map Feature - Enhanced Database Schema
-- This file contains the complete database schema for the Data Map feature

-- ============================================
-- 1. User Recent Views (Already exists in main schema.sql)
-- ============================================
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

-- ============================================
-- 2. Data Map Search History
-- ============================================
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

-- ============================================
-- 3. Data Map Favorites/Bookmarks
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_user_favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `resource_type` VARCHAR(50) NOT NULL COMMENT 'TABLE, DATASOURCE, DATABASE',
  `resource_id` BIGINT NOT NULL COMMENT 'ID of the favorited resource',
  `resource_name` VARCHAR(500) NOT NULL,
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id',
  `tags` VARCHAR(500) COMMENT 'User-defined tags, comma-separated',
  `notes` TEXT COMMENT 'User notes',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_resource` (`username`, `resource_type`, `resource_id`),
  INDEX `idx_username` (`username`),
  INDEX `idx_datasource` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏夹';

-- ============================================
-- 4. Data Map Statistics Cache
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_datamap_stats_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `datasource_id` BIGINT COMMENT 'FK to data_source_config.id, NULL for global stats',
  `stat_type` VARCHAR(50) NOT NULL COMMENT 'INSTANCE, DATABASE, TABLE, API, COLLECTOR',
  `stat_name` VARCHAR(100) NOT NULL,
  `stat_value` BIGINT NOT NULL,
  `calculated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_datasource_stat` (`datasource_id`, `stat_type`, `stat_name`),
  INDEX `idx_stat_type` (`stat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据地图统计信息缓存';

-- ============================================
-- 5. Data Catalog Categories (for classification)
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_data_categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(100) NOT NULL,
  `parent_id` BIGINT COMMENT 'FK to self for hierarchical categories',
  `description` TEXT,
  `icon` VARCHAR(100),
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据目录分类';

-- ============================================
-- 6. Table Category Mapping
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_table_category_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `table_id` BIGINT NOT NULL COMMENT 'FK to dga_table_metadata.id',
  `category_id` BIGINT NOT NULL COMMENT 'FK to dga_data_categories.id',
  `assigned_by` VARCHAR(100),
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_category` (`table_id`, `category_id`),
  INDEX `idx_table` (`table_id`),
  INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表与分类关联';

-- ============================================
-- 7. Data Map User Permissions (for row-level access control)
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_datamap_permissions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `resource_type` VARCHAR(50) NOT NULL COMMENT 'DATASOURCE, DATABASE, TABLE',
  `resource_id` BIGINT NOT NULL,
  `permission_level` VARCHAR(50) NOT NULL COMMENT 'VIEW, QUERY, EXPORT, MANAGE',
  `granted_by` VARCHAR(100),
  `granted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME COMMENT 'NULL for no expiration',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_resource_perm` (`username`, `resource_type`, `resource_id`, `permission_level`),
  INDEX `idx_username` (`username`),
  INDEX `idx_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据地图权限管理';

-- ============================================
-- 8. Data Lineage (for tracking data flow)
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_data_lineage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_table_id` BIGINT NOT NULL COMMENT 'FK to dga_table_metadata.id',
  `target_table_id` BIGINT NOT NULL COMMENT 'FK to dga_table_metadata.id',
  `lineage_type` VARCHAR(50) NOT NULL COMMENT 'ETL, VIEW, COPY, DERIVED',
  `transformation_logic` TEXT COMMENT 'SQL or description of transformation',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_source` (`source_table_id`),
  INDEX `idx_target` (`target_table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘关系';

-- ============================================
-- 9. Data Map Recommendations (for AI search)
-- ============================================
CREATE TABLE IF NOT EXISTS `dga_search_recommendations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `recommended_resource_type` VARCHAR(50) NOT NULL,
  `recommended_resource_id` BIGINT NOT NULL,
  `recommended_resource_name` VARCHAR(500) NOT NULL,
  `recommendation_reason` VARCHAR(500) COMMENT 'Why this is recommended',
  `score` DOUBLE COMMENT 'Recommendation score',
  `is_clicked` BOOLEAN DEFAULT FALSE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_username` (`username`, `created_at` DESC),
  INDEX `idx_score` (`score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索推荐记录';

-- ============================================
-- Sample Data for Categories
-- ============================================
INSERT INTO `dga_data_categories` (`category_name`, `parent_id`, `description`, `icon`, `sort_order`) VALUES
('业务数据', NULL, '业务相关的数据表', 'shop', 1),
('用户数据', NULL, '用户信息相关数据', 'user', 2),
('交易数据', NULL, '交易和订单数据', 'transaction', 3),
('日志数据', NULL, '系统和业务日志', 'file-text', 4),
('报表数据', NULL, '统计和报表数据', 'bar-chart', 5);

-- ============================================
-- Sample Data for Stats Cache
-- ============================================
INSERT INTO `dga_datamap_stats_cache` (`datasource_id`, `stat_type`, `stat_name`, `stat_value`) VALUES
(NULL, 'INSTANCE', 'total_instances', 0),
(NULL, 'DATABASE', 'total_databases', 0),
(NULL, 'TABLE', 'total_tables', 0),
(NULL, 'TABLE', 'managed_tables', 0),
(NULL, 'API', 'total_apis', 0),
(NULL, 'COLLECTOR', 'total_collectors', 0);
