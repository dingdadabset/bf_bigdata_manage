# Azkaban Schema Notes

Last verified: 2026-05-08

Configured test endpoint:

- JDBC URL: `jdbc:mysql://10.0.20.108:3306/azkaban`
- Username: `cs_hadoop`
- Source config table: `dga_cluster_endpoint`

These notes capture the actual Azkaban schema observed in the configured environment so scheduler-related code can avoid guessing field names.

## `projects`

Observed columns:

- `id` `int(11)` primary key
- `name` `varchar(64)`
- `active` `tinyint(1)`
- `modified_time` `bigint(20)`
- `create_time` `bigint(20)`
- `version` `int(11)`
- `last_modified_by` `varchar(64)`
- `description` `varchar(2048)`
- `enc_type` `tinyint(4)`
- `settings_blob` `longblob`

Important compatibility note:

- This environment does **not** have `created_by`.
- Project owner fallback should prefer `last_modified_by`.

## `execution_flows`

Observed columns:

- `exec_id` `int(11)` primary key
- `project_id` `int(11)`
- `version` `int(11)`
- `flow_id` `varchar(128)`
- `status` `tinyint(4)`
- `submit_user` `varchar(64)`
- `submit_time` `bigint(20)`
- `update_time` `bigint(20)`
- `start_time` `bigint(20)`
- `end_time` `bigint(20)`
- `enc_type` `tinyint(4)`
- `flow_data` `longblob`
- `executor_id` `int(11)`

Usage notes:

- `submit_user` exists and can be displayed in recent run history.
- `start_time` is indexed in this environment and is a better driver for "recent executions" queries than full scans on `execution_jobs`.

## `execution_jobs`

Observed columns:

- `exec_id` `int(11)` primary key component
- `project_id` `int(11)`
- `version` `int(11)`
- `flow_id` `varchar(128)` primary key component
- `job_id` `varchar(128)` primary key component
- `attempt` `int(11)` primary key component
- `start_time` `bigint(20)`
- `end_time` `bigint(20)`
- `status` `tinyint(4)`
- `input_params` `longblob`
- `output_params` `longblob`
- `attachments` `longblob`

Usage notes:

- Full aggregation over all `execution_jobs` rows is expensive.
- Prefer querying a recent slice of `execution_flows`, then joining back to `execution_jobs`.

## Scheduler page query strategy

Recommended approach for task overview:

1. Read a recent window from `execution_flows` ordered by `start_time DESC`.
2. Join the recent execution ids back to `execution_jobs`.
3. Join `projects` for project metadata.
4. Aggregate by `project_name + flow_id + job_id`.

This avoids large sorts on the full `execution_jobs` table and matches the current environment better than using `projects.created_by`.
