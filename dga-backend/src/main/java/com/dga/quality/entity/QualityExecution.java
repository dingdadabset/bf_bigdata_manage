package com.dga.quality.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_quality_execution")
public class QualityExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED, WARNING

    @Column(name = "result_value")
    private Double resultValue; // Actual metric value

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @PrePersist
    protected void onExecute() {
        executedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getResultValue() {
        return resultValue;
    }

    public void setResultValue(Double resultValue) {
        this.resultValue = resultValue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
