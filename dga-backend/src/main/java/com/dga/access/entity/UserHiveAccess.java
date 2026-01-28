package com.dga.access.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_hive_access")
@EntityListeners(AuditingEntityListener.class)
public class UserHiveAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "cluster_name", nullable = false)
    private String clusterName;

    @Column(name = "database_name", nullable = false)
    private String databaseName;

    @Column(name = "table_name")
    private String tableName; // Nullable, if null means DB level permission

    @Column(nullable = false)
    private String permission; // e.g. "SELECT,UPDATE" or "ALL"

    @Column(name = "grant_time")
    @CreatedDate
    private LocalDateTime grantTime;
    
    @Column(name = "update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;

    @Column(name = "granted_by")
    private String grantedBy; // Who performed the operation

    @Column(name = "revoke_time")
    private LocalDateTime revokeTime;

    @Column(nullable = false)
    private String status; // ACTIVE, REVOKED

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = "ACTIVE";
        }
    }

    // Getters and Setters
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

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public LocalDateTime getGrantTime() {
        return grantTime;
    }

    public void setGrantTime(LocalDateTime grantTime) {
        this.grantTime = grantTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
