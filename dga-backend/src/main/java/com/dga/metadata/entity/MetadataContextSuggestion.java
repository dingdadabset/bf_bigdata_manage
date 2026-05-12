package com.dga.metadata.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_metadata_context_suggestion",
        indexes = {
                @Index(name = "idx_context_suggestion_table", columnList = "table_id, status, context_type"),
                @Index(name = "idx_context_suggestion_column", columnList = "column_id, status"),
                @Index(name = "idx_context_suggestion_source", columnList = "source_endpoint_id, data_source_id, run_id")
        })
public class MetadataContextSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "column_id")
    private Long columnId;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_endpoint_id", nullable = false)
    private Long sourceEndpointId;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "flow_name")
    private String flowName;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "context_type", nullable = false)
    private String contextType;

    @Lob
    @Column(name = "suggested_value")
    private String suggestedValue;

    @Lob
    @Column(name = "evidence")
    private String evidence;

    @Column(name = "confidence")
    private String confidence;

    @Column(name = "status")
    private String status;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by")
    private String appliedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceEndpointId() { return sourceEndpointId; }
    public void setSourceEndpointId(Long sourceEndpointId) { this.sourceEndpointId = sourceEndpointId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getFlowName() { return flowName; }
    public void setFlowName(String flowName) { this.flowName = flowName; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }
    public String getSuggestedValue() { return suggestedValue; }
    public void setSuggestedValue(String suggestedValue) { this.suggestedValue = suggestedValue; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public String getAppliedBy() { return appliedBy; }
    public void setAppliedBy(String appliedBy) { this.appliedBy = appliedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
