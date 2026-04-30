# 数据库变更记录

本项目暂未接入 Flyway / Liquibase，生产或测试环境需要手工执行 `dga-backend/src/main/resources/migration-*.sql` 中的迁移脚本。

## 2026-04-30 权限回收操作人审计

背景：

- 权限治理需要记录授权与回收的实际操作人，避免历史记录只知道授权人，不知道回收人。

脚本：

- `dga-backend/src/main/resources/migration-user-resource-access-revoked-by.sql`

变更：

- `user_resource_access` 增加 `revoked_by` 字段，用于记录执行回收操作的平台用户。

## 2026-04-29 元数据搜索索引优化

背景：

- 数据地图改为“搜索后查表”，避免进入页面时全量查询所有 Hive 表。
- `/api/metadata/search` 会关联表、字段、业务元数据、标签、指标等信息，数据量增长后需要补充索引降低筛选和分页成本。

脚本：

- `dga-backend/src/main/resources/migration-metadata-search-indexes.sql`

变更：

- `data_source_config` 增加 `idx_ds_search_scope(type, endpoint_id, is_deleted, id)`。
- `meta_table_info` 增加 `idx_meta_search_filters(datasource_id, db_name, owner, lifecycle_status, sync_time)`。
- `meta_table_info` 增加 `idx_meta_source_owner(source_owner)`。
- `meta_table_info` 增加 `idx_meta_table_name(table_name)`。
- `meta_column_info` 增加 `idx_col_name_table(column_name, table_id)`。
- `dga_metric_definition` 增加 `idx_metric_table_status(table_id, status)`。

执行注意：

- 如果某个索引已经存在，跳过对应 `ALTER TABLE`。
- 当前搜索仍支持字段名、字段备注、业务口径、标签、指标等模糊命中；后续如果表量继续增长，建议升级为 MySQL FULLTEXT 或单独建设搜索索引表。
