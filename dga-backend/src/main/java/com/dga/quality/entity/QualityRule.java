package com.dga.quality.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_quality_rule")
public class QualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "datasource_id")
    private Long dataSourceId;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "column_name")
    private String columnName; // Can be null if rule applies to whole table

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // NULL_RATE, UNIQUE_RATE, VALUE_RANGE, ROW_COUNT, FRESHNESS, REGEX_MATCH

    @Column(name = "severity")
    private String severity;

    @Column(name = "status")
    private String status;

    @Column(name = "scan_scope")
    private String scanScope;

    @Column(name = "threshold")
    private Double threshold; // e.g., 0.05 (5% failure rate allowed)

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "regex_pattern", length = 1000)
    private String regexPattern;

    @Column(name = "action_type")
    private String actionType; // ALARM, BLOCK_JOB

    @Column(name = "owner")
    private String owner;

    @Column(name = "last_execution_status")
    private String lastExecutionStatus;

    @Column(name = "last_result_value")
    private Double lastResultValue;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        }
        if (severity == null || severity.trim().isEmpty()) {
            severity = "MEDIUM";
        }
        if (scanScope == null || scanScope.trim().isEmpty()) {
            scanScope = "LATEST_PARTITION";
        }
        if (actionType == null || actionType.trim().isEmpty()) {
            actionType = "ALARM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
