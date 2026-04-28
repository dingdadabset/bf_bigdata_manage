package com.dga.metadata.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_metadata_collection_task")
public class MetadataCollectionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "datasource_name")
    private String dataSourceName;

    @Column(name = "cluster_code")
    private String clusterCode;

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

    @Column(name = "success_table_count")
    private Integer successTableCount = 0;

    @Column(name = "failed_table_count")
    private Integer failedTableCount = 0;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getSuccessTableCount() {
        return successTableCount;
    }

    public void setSuccessTableCount(Integer successTableCount) {
        this.successTableCount = successTableCount;
    }

    public Integer getFailedTableCount() {
        return failedTableCount;
    }

    public void setFailedTableCount(Integer failedTableCount) {
        this.failedTableCount = failedTableCount;
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
