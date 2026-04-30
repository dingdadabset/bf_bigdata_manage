package com.dga.access.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_resource_access")
@EntityListeners(AuditingEntityListener.class)
public class UserResourceAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "cluster_code")
    private String clusterCode;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(name = "engine_type")
    private String engineType;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name = "table_name")
    private String tableName;

    @Column(nullable = false)
    private String permission;

    @Column(name = "auth_backend")
    private String authBackend;

    @Column(name = "source")
    private String source;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "collaborator_owners", length = 1000)
    private String collaboratorOwners;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_due_at")
    private LocalDateTime reviewDueAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "granted_by")
    private String grantedBy;

    @Column(name = "revoked_by")
    private String revokedBy;

    @Column(name = "grant_time")
    @CreatedDate
    private LocalDateTime grantTime;

    @Column(name = "revoke_time")
    private LocalDateTime revokeTime;

    @Column(name = "update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = "ACTIVE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
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

    public String getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(String authBackend) {
        this.authBackend = authBackend;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewDueAt() {
        return reviewDueAt;
    }

    public void setReviewDueAt(LocalDateTime reviewDueAt) {
        this.reviewDueAt = reviewDueAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getGrantTime() {
        return grantTime;
    }

    public void setGrantTime(LocalDateTime grantTime) {
        this.grantTime = grantTime;
    }

    public LocalDateTime getRevokeTime() {
        return revokeTime;
    }

    public void setRevokeTime(LocalDateTime revokeTime) {
        this.revokeTime = revokeTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
