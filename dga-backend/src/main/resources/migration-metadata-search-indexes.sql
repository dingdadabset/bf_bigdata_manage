-- Manual migration for metadata search performance.
-- Execute once on MySQL. If an index already exists, skip the corresponding ALTER TABLE.

ALTER TABLE data_source_config
  ADD INDEX idx_ds_search_scope (type, endpoint_id, is_deleted, id);

ALTER TABLE meta_table_info
  ADD INDEX idx_meta_search_filters (datasource_id, db_name, owner, lifecycle_status, sync_time);

ALTER TABLE meta_table_info
  ADD INDEX idx_meta_source_owner (source_owner);

ALTER TABLE meta_table_info
  ADD INDEX idx_meta_table_name (table_name);

ALTER TABLE meta_column_info
  ADD INDEX idx_col_name_table (column_name, table_id);

ALTER TABLE dga_metric_definition
  ADD INDEX idx_metric_table_status (table_id, status);
