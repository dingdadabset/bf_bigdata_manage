package com.dga.scheduler.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_scheduler_task_context",
        indexes = {
                @Index(name = "idx_scheduler_context_scope", columnList = "cluster_code, source_endpoint_id, project_name, flow_name"),
                @Index(name = "idx_scheduler_context_task", columnList = "cluster_code, source_endpoint_id, project_name, task_name"),
                @Index(name = "idx_scheduler_context_run", columnList = "source_endpoint_id, data_source_id, run_id")
        })
public class SchedulerTaskContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_code", nullable = false)
    private String clusterCode;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_endpoint_id", nullable = false)
    private Long sourceEndpointId;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "flow_name")
    private String flowName;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "task_key", length = 500)
    private String taskKey;

    @Column(name = "job_path", length = 1000)
    private String jobPath;

    @Lob
    @Column(name = "command_text")
    private String commandText;

    @Lob
    @Column(name = "input_tables")
    private String inputTables;

    @Lob
    @Column(name = "output_tables")
    private String outputTables;

    @Column(name = "matched_table_ids", length = 2000)
    private String matchedTableIds;

    @Column(name = "parse_status")
    private String parseStatus;

    @Column(name = "parse_message", length = 1000)
    private String parseMessage;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "status")
    private String status;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClusterCode() { return clusterCode; }
    public void setClusterCode(String clusterCode) { this.clusterCode = clusterCode; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceEndpointId() { return sourceEndpointId; }
    public void setSourceEndpointId(Long sourceEndpointId) { this.sourceEndpointId = sourceEndpointId; }
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getFlowName() { return flowName; }
    public void setFlowName(String flowName) { this.flowName = flowName; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public String getJobPath() { return jobPath; }
    public void setJobPath(String jobPath) { this.jobPath = jobPath; }
    public String getCommandText() { return commandText; }
    public void setCommandText(String commandText) { this.commandText = commandText; }
    public String getInputTables() { return inputTables; }
    public void setInputTables(String inputTables) { this.inputTables = inputTables; }
    public String getOutputTables() { return outputTables; }
    public void setOutputTables(String outputTables) { this.outputTables = outputTables; }
    public String getMatchedTableIds() { return matchedTableIds; }
    public void setMatchedTableIds(String matchedTableIds) { this.matchedTableIds = matchedTableIds; }
    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }
    public String getParseMessage() { return parseMessage; }
    public void setParseMessage(String parseMessage) { this.parseMessage = parseMessage; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
