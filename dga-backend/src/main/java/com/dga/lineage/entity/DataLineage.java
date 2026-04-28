package com.dga.lineage.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_data_lineage")
@EntityListeners(AuditingEntityListener.class)
public class DataLineage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_table_id", nullable = false)
    private Long sourceTableId;

    @Column(name = "target_table_id", nullable = false)
    private Long targetTableId;

    @Column(name = "lineage_type", nullable = false)
    private String lineageType; // ETL, VIEW, COPY, DERIVED

    @Column(name = "transformation_logic")
    private String transformationLogic; // SQL or description

    @Column(name = "source_type")
    private String sourceType; // AZKABAN_DB, DOLPHINSCHEDULER_DB, LEGACY

    @Column(name = "source_endpoint_id")
    private Long sourceEndpointId;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "cluster_code")
    private String clusterCode;

    @Column(name = "source_project")
    private String sourceProject;

    @Column(name = "source_workflow")
    private String sourceWorkflow;

    @Column(name = "source_task")
    private String sourceTask;

    @Column(name = "source_task_key")
    private String sourceTaskKey;

    @Column(name = "source_sql_hash")
    private String sourceSqlHash;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "status")
    private String status;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceTableId() {
        return sourceTableId;
    }

    public void setSourceTableId(Long sourceTableId) {
        this.sourceTableId = sourceTableId;
    }

    public Long getTargetTableId() {
        return targetTableId;
    }

    public void setTargetTableId(Long targetTableId) {
        this.targetTableId = targetTableId;
    }

    public String getLineageType() {
        return lineageType;
    }

    public void setLineageType(String lineageType) {
        this.lineageType = lineageType;
    }

    public String getTransformationLogic() {
        return transformationLogic;
    }

    public void setTransformationLogic(String transformationLogic) {
        this.transformationLogic = transformationLogic;
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

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getSourceProject() {
        return sourceProject;
    }

    public void setSourceProject(String sourceProject) {
        this.sourceProject = sourceProject;
    }

    public String getSourceWorkflow() {
        return sourceWorkflow;
    }

    public void setSourceWorkflow(String sourceWorkflow) {
        this.sourceWorkflow = sourceWorkflow;
    }

    public String getSourceTask() {
        return sourceTask;
    }

    public void setSourceTask(String sourceTask) {
        this.sourceTask = sourceTask;
    }

    public String getSourceTaskKey() {
        return sourceTaskKey;
    }

    public void setSourceTaskKey(String sourceTaskKey) {
        this.sourceTaskKey = sourceTaskKey;
    }

    public String getSourceSqlHash() {
        return sourceSqlHash;
    }

    public void setSourceSqlHash(String sourceSqlHash) {
        this.sourceSqlHash = sourceSqlHash;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getParsedAt() {
        return parsedAt;
    }

    public void setParsedAt(LocalDateTime parsedAt) {
        this.parsedAt = parsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
