ALTER TABLE dga_cluster_endpoint
    ADD COLUMN driver_profile VARCHAR(50) DEFAULT 'MODERN' COMMENT 'MODERN, LEGACY_CDH5, AUTO; only for HIVE_SERVER2';

UPDATE dga_cluster_endpoint
SET driver_profile = 'MODERN'
WHERE endpoint_type = 'HIVE_SERVER2'
  AND (driver_profile IS NULL OR driver_profile = '');
