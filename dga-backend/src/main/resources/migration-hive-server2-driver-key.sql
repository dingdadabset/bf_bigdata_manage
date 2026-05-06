ALTER TABLE dga_cluster_endpoint
    ADD COLUMN driver_key VARCHAR(100) DEFAULT 'builtin-modern' COMMENT 'Selected Hive JDBC driver key for HIVE_SERVER2';

UPDATE dga_cluster_endpoint
SET driver_key = CASE
    WHEN endpoint_type <> 'HIVE_SERVER2' THEN NULL
    WHEN driver_profile = 'LEGACY_CDH5' THEN 'legacy-cdh5'
    ELSE 'builtin-modern'
END
WHERE driver_key IS NULL;
