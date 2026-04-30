# 数据库变更记录

本项目暂未接入 Flyway / Liquibase，生产或测试环境需要手工执行 `dga-backend/src/main/resources/migration-*.sql` 中的迁移脚本。

## 2026-04-30 权限治理风险基线

背景：

- 权限治理需要发现沉默账号、外包/临时账号过期时间缺失、高权限待复核、无人负责权限等风险，并沉淀认领/复核/关闭闭环。
- 活跃度改为真实来源采集 + 证据可追溯：支持 LDAP、Ranger、HiveServer2、StarRocks、Doris、HDFS、YARN、Hue、DGA 多选扫描；未配置来源标记为未接入，不参与判定。
- HDP Ranger Audit 支持通过 `RANGER_DB` 端点连接 Ranger MySQL 审计库，自动识别 `x_access_audit` / `xa_access_audit`，按 `request_user/repo_name/resource_path/access_type/event_time` 判定 Hive 权限是否长期未使用。
- 沉默账号按多来源 `last_active_at` 最大值判定；没有真实来源活动时间时，使用 DGA 用户更新时间/创建时间兜底并标低置信度。
- 权限负责人升级为主负责人 + 协同负责人模型；负责人可来自平台用户/LDAP 搜索，也可手工创建负责人档案。

脚本：

- `dga-backend/src/main/resources/migration-access-governance-v1.sql`

变更：

- `dga_users` 增加 `user_type`、`expires_at`、`last_active_at`、`last_active_source`。
- `user_resource_access` 增加 `owner`、`collaborator_owners`、`last_reviewed_at`、`reviewed_by`、`review_due_at`。
- 新增 `access_activity_evidence`，记录账号、集群、来源系统、最近活跃时间、证据摘要、置信度和采集时间。
- 新增 `access_owner`，记录负责人编码、显示名、邮箱、来源、状态、创建人和更新时间。
- 新增 `access_governance_issue`，记录风险类型、严重级别、状态、证据、建议、来源系统、最近活跃来源/时间、置信度、owner/assignee、解决人和解决时间。

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
