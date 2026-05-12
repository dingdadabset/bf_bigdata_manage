package com.dga.scheduler.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_scheduler_task_owner",
        uniqueConstraints = @UniqueConstraint(name = "uk_scheduler_owner_scope",
                columnNames = {"cluster_code", "source_endpoint_id", "project_name", "flow_name", "task_name"}),
        indexes = {
                @Index(name = "idx_scheduler_owner_cluster", columnList = "cluster_code, source_endpoint_id"),
                @Index(name = "idx_scheduler_owner_owner", columnList = "owner, status")
        })
public class SchedulerTaskOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_code", nullable = false, length = 100)
    private String clusterCode;

    @Column(name = "source_endpoint_id", nullable = false)
    private Long sourceEndpointId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "flow_name", nullable = false)
    private String flowName;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "collaborator_owners", length = 1000)
    private String collaboratorOwners;

    @Column(name = "owner_source", length = 50)
    private String ownerSource;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClusterCode() { return clusterCode; }
    public void setClusterCode(String clusterCode) { this.clusterCode = clusterCode; }
    public Long getSourceEndpointId() { return sourceEndpointId; }
    public void setSourceEndpointId(Long sourceEndpointId) { this.sourceEndpointId = sourceEndpointId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getFlowName() { return flowName; }
    public void setFlowName(String flowName) { this.flowName = flowName; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getCollaboratorOwners() { return collaboratorOwners; }
    public void setCollaboratorOwners(String collaboratorOwners) { this.collaboratorOwners = collaboratorOwners; }
    public String getOwnerSource() { return ownerSource; }
    public void setOwnerSource(String ownerSource) { this.ownerSource = ownerSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
