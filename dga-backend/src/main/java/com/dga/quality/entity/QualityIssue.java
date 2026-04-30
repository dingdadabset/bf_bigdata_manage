package com.dga.quality.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_quality_issue")
public class QualityIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "issue_title")
    private String issueTitle;

    @Column(name = "issue_description", length = 1000)
    private String issueDescription;

    @Column(name = "status")
    private String status;

    @Column(name = "severity")
    private String severity;

    @Column(name = "owner")
    private String owner;

    @Column(name = "result_value")
    private Double resultValue;

    @Column(name = "threshold")
    private Double threshold;

    @Column(name = "last_execution_id")
    private Long lastExecutionId;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
        if (status == null || status.trim().isEmpty()) {
            status = "OPEN";
        }
    }
}
