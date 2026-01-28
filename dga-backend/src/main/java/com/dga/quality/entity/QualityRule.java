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

    @Column(name = "column_name")
    private String columnName; // Can be null if rule applies to whole table

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // NULL_CHECK, UNIQUENESS, VALUE_RANGE, REGEX_MATCH

    @Column(name = "threshold")
    private Double threshold; // e.g., 0.05 (5% failure rate allowed)

    @Column(name = "action_type")
    private String actionType; // ALARM, BLOCK_JOB

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
