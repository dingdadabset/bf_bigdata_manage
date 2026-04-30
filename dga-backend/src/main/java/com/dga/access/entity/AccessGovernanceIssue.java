package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_governance_issue",
        indexes = {
                @Index(name = "idx_access_issue_status", columnList = "status, severity"),
                @Index(name = "idx_access_issue_user", columnList = "username, cluster_name"),
                @Index(name = "idx_access_issue_key", columnList = "issue_key")
        })
public class AccessGovernanceIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_key", nullable = false, length = 255)
    private String issueKey;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "cluster_code", length = 100)
    private String clusterCode;

    @Column(name = "cluster_name", length = 255)
    private String clusterName;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "database_name", length = 255)
    private String databaseName;

    @Column(name = "table_name", length = 255)
    private String tableName;

    @Column(name = "permission", length = 100)
    private String permission;

    @Column(name = "access_id")
    private Long accessId;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "collaborator_owners", length = 1000)
    private String collaboratorOwners;

    @Column(name = "assignee", length = 100)
    private String assignee;

    @Column(name = "source_systems", length = 255)
    private String sourceSystems;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "last_active_source", length = 50)
    private String lastActiveSource;

    @Column(name = "confidence", length = 20)
    private String confidence;

    @Column(name = "evidence", length = 1000)
    private String evidence;

    @Column(name = "recommendation", length = 1000)
    private String recommendation;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null || status.trim().isEmpty()) {
            status = "OPEN";
        }
        if (detectedAt == null) {
            detectedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Long getAccessId() {
        return accessId;
    }

    public void setAccessId(Long accessId) {
        this.accessId = accessId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCollaboratorOwners() {
        return collaboratorOwners;
    }

    public void setCollaboratorOwners(String collaboratorOwners) {
        this.collaboratorOwners = collaboratorOwners;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getSourceSystems() {
        return sourceSystems;
    }

    public void setSourceSystems(String sourceSystems) {
        this.sourceSystems = sourceSystems;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getLastActiveSource() {
        return lastActiveSource;
    }

    public void setLastActiveSource(String lastActiveSource) {
        this.lastActiveSource = lastActiveSource;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }
}
