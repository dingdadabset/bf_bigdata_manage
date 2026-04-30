-- Manual cleanup for legacy Hive metadata that is not bound to an Environment Resource endpoint.
-- Run this after verifying the preview result. It keeps valid HIVE_METASTORE_DB-backed
-- metadata and removes orphan/history metadata from the first-stage governance pages.

-- Preview legacy data sources and related table counts.
SELECT
    ds.id,
    ds.name,
    ds.type,
    ds.cluster_code,
    ds.endpoint_id,
    ds.status,
    ds.is_deleted,
    COUNT(t.id) AS metadata_table_count
FROM data_source_config ds
LEFT JOIN meta_table_info t ON t.datasource_id = ds.id
WHERE ds.type = 'HIVE'
  AND (ds.endpoint_id IS NULL OR ds.is_deleted = TRUE)
GROUP BY ds.id, ds.name, ds.type, ds.cluster_code, ds.endpoint_id, ds.status, ds.is_deleted
ORDER BY ds.id;

-- Uncomment the following block when the preview is correct.
/*
CREATE TEMPORARY TABLE tmp_legacy_hive_datasource_ids AS
SELECT ds.id
FROM data_source_config ds
WHERE ds.type = 'HIVE'
  AND (ds.endpoint_id IS NULL OR ds.is_deleted = TRUE);

CREATE TEMPORARY TABLE tmp_legacy_hive_table_ids AS
SELECT t.id
FROM meta_table_info t
JOIN tmp_legacy_hive_datasource_ids legacy_ds ON legacy_ds.id = t.datasource_id;

DELETE l
FROM dga_data_lineage l
JOIN tmp_legacy_hive_table_ids legacy_t
  ON legacy_t.id = l.source_table_id OR legacy_t.id = l.target_table_id;

DELETE p
FROM meta_partition_info p
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = p.table_id;

DELETE c
FROM meta_column_info c
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = c.table_id;

DELETE b
FROM dga_table_business_metadata b
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = b.table_id;

DELETE tm
FROM dga_table_tag_mapping tm
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = tm.table_id;

DELETE m
FROM dga_metric_definition m
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = m.table_id;

DELETE task
FROM dga_metadata_collection_task task
JOIN tmp_legacy_hive_datasource_ids legacy_ds ON legacy_ds.id = task.datasource_id;

DELETE t
FROM meta_table_info t
JOIN tmp_legacy_hive_table_ids legacy_t ON legacy_t.id = t.id;

UPDATE data_source_config ds
JOIN tmp_legacy_hive_datasource_ids legacy_ds ON legacy_ds.id = ds.id
SET ds.is_deleted = TRUE,
    ds.status = 'INACTIVE';

DROP TEMPORARY TABLE IF EXISTS tmp_legacy_hive_table_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_legacy_hive_datasource_ids;
*/
