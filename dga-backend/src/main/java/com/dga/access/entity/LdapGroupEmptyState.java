package com.dga.access.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "ldap_group_empty_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ldap_group_empty_state", columnNames = {"cluster_name", "group_name"})
        },
        indexes = {
                @Index(name = "idx_ldap_group_empty_cluster", columnList = "cluster_name, empty_since"),
                @Index(name = "idx_ldap_group_empty_code", columnList = "cluster_code, group_name")
        })
public class LdapGroupEmptyState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_code", length = 100)
    private String clusterCode;

    @Column(name = "cluster_name", length = 255, nullable = false)
    private String clusterName;

    @Column(name = "group_name", length = 128, nullable = false)
    private String groupName;

    @Column(name = "gid_number")
    private Long gidNumber;

    @Column(name = "empty_since")
    private LocalDateTime emptySince;

    @Column(name = "last_seen_empty_at")
    private LocalDateTime lastSeenEmptyAt;

    @Column(name = "last_seen_non_empty_at")
    private LocalDateTime lastSeenNonEmptyAt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getGidNumber() {
        return gidNumber;
    }

    public void setGidNumber(Long gidNumber) {
        this.gidNumber = gidNumber;
    }

    public LocalDateTime getEmptySince() {
        return emptySince;
    }

    public void setEmptySince(LocalDateTime emptySince) {
        this.emptySince = emptySince;
    }

    public LocalDateTime getLastSeenEmptyAt() {
        return lastSeenEmptyAt;
    }

    public void setLastSeenEmptyAt(LocalDateTime lastSeenEmptyAt) {
        this.lastSeenEmptyAt = lastSeenEmptyAt;
    }

    public LocalDateTime getLastSeenNonEmptyAt() {
        return lastSeenNonEmptyAt;
    }

    public void setLastSeenNonEmptyAt(LocalDateTime lastSeenNonEmptyAt) {
        this.lastSeenNonEmptyAt = lastSeenNonEmptyAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
