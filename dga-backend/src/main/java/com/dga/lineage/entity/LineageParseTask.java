package com.dga.lineage.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_lineage_parse_task")
public class LineageParseTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_endpoint_id")
    private Long sourceEndpointId;

    @Column(name = "source_endpoint_name")
    private String sourceEndpointName;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "data_source_name")
    private String dataSourceName;

    @Column(name = "cluster_code")
    private String clusterCode;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "status")
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "success_edge_count")
    private Integer successEdgeCount;

    @Column(name = "failed_edge_count")
    private Integer failedEdgeCount;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "RUNNING";
        }
        if (successEdgeCount == null) {
            successEdgeCount = 0;
        }
        if (failedEdgeCount == null) {
            failedEdgeCount = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceEndpointId() {
        return sourceEndpointId;
    }

    public void setSourceEndpointId(Long sourceEndpointId) {
        this.sourceEndpointId = sourceEndpointId;
    }

    public String getSourceEndpointName() {
        return sourceEndpointName;
    }

    public void setSourceEndpointName(String sourceEndpointName) {
        this.sourceEndpointName = sourceEndpointName;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getSuccessEdgeCount() {
        return successEdgeCount;
    }

    public void setSuccessEdgeCount(Integer successEdgeCount) {
        this.successEdgeCount = successEdgeCount;
    }

    public Integer getFailedEdgeCount() {
        return failedEdgeCount;
    }

    public void setFailedEdgeCount(Integer failedEdgeCount) {
        this.failedEdgeCount = failedEdgeCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }
}
