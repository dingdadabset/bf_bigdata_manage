-- Data quality v1 manual migration.
-- Run once on environments that already have dga_quality_rule / dga_quality_execution.

ALTER TABLE dga_quality_rule
    ADD COLUMN datasource_id BIGINT NULL,
    ADD COLUMN db_name VARCHAR(255) NULL,
    ADD COLUMN table_name VARCHAR(255) NULL,
    ADD COLUMN rule_name VARCHAR(255) NULL,
    ADD COLUMN severity VARCHAR(20) DEFAULT 'MEDIUM',
    ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN scan_scope VARCHAR(50) DEFAULT 'LATEST_PARTITION',
    ADD COLUMN expected_value VARCHAR(100) NULL,
    ADD COLUMN min_value DOUBLE NULL,
    ADD COLUMN max_value DOUBLE NULL,
    ADD COLUMN regex_pattern VARCHAR(1000) NULL,
    ADD COLUMN owner VARCHAR(100) NULL,
    ADD COLUMN last_execution_status VARCHAR(20) NULL,
    ADD COLUMN last_result_value DOUBLE NULL,
    ADD COLUMN last_error_message VARCHAR(1000) NULL,
    ADD COLUMN last_executed_at DATETIME NULL,
    ADD COLUMN created_by VARCHAR(100) NULL,
    ADD COLUMN updated_at DATETIME NULL;

CREATE INDEX idx_quality_rule_table ON dga_quality_rule (table_id);
CREATE INDEX idx_quality_rule_datasource ON dga_quality_rule (datasource_id);
CREATE INDEX idx_quality_rule_status ON dga_quality_rule (status);

ALTER TABLE dga_quality_execution
    ADD COLUMN table_id BIGINT NULL,
    ADD COLUMN threshold DOUBLE NULL,
    ADD COLUMN scan_scope VARCHAR(50) NULL,
    ADD COLUMN scan_filter VARCHAR(1000) NULL,
    ADD COLUMN executed_sql VARCHAR(4000) NULL,
    ADD COLUMN executed_by VARCHAR(100) NULL,
    ADD COLUMN duration_ms BIGINT NULL;

CREATE INDEX idx_quality_exec_table ON dga_quality_execution (table_id);
CREATE INDEX idx_quality_exec_status_time ON dga_quality_execution (status, executed_at);

CREATE TABLE IF NOT EXISTS dga_quality_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rule_id BIGINT NOT NULL,
  table_id BIGINT NULL,
  issue_title VARCHAR(255) NULL,
  issue_description VARCHAR(1000) NULL,
  status VARCHAR(20) DEFAULT 'OPEN',
  severity VARCHAR(20) NULL,
  owner VARCHAR(100) NULL,
  result_value DOUBLE NULL,
  threshold DOUBLE NULL,
  last_execution_id BIGINT NULL,
  first_seen_at DATETIME NULL,
  last_seen_at DATETIME NULL,
  resolved_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX idx_quality_issue_rule_status (rule_id, status),
  INDEX idx_quality_issue_table_status (table_id, status),
  INDEX idx_quality_issue_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE dga_quality_rule r
JOIN meta_table_info t ON t.id = r.table_id
SET r.datasource_id = t.datasource_id,
    r.db_name = t.db_name,
    r.table_name = t.table_name,
    r.rule_name = COALESCE(r.rule_name, CONCAT(t.db_name, '.', t.table_name, ' ', r.rule_type)),
    r.owner = COALESCE(r.owner, t.owner, t.source_owner),
    r.updated_at = COALESCE(r.updated_at, r.created_at, NOW())
WHERE r.datasource_id IS NULL OR r.db_name IS NULL OR r.table_name IS NULL;
