package com.dga.metadata.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "meta_table_info")
public class TableMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "cluster_code")
    private String clusterCode;

    @Column(name = "db_name", nullable = false)
    private String dbName;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "table_comment", length = 1000)
    private String tableComment;

    @Column(name = "source_owner")
    private String sourceOwner;

    @Column(name = "owner")
    private String owner;

    @Column(name = "owner_source")
    private String ownerSource;

    @Column(name = "storage_format")
    private String storageFormat; // e.g., ORC, PARQUET, TEXTFILE

    @Column(name = "hdfs_path")
    private String locationPath;

    @Column(name = "table_size")
    private Long totalSize; // in bytes

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "partition_count")
    private Long partitionCount;

    @Column(name = "lifecycle_status")
    private String lifecycleStatus;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "last_modify_time")
    private LocalDateTime updatedAt;

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

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public void setSourceOwner(String sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerSource() {
        return ownerSource;
    }

    public void setOwnerSource(String ownerSource) {
        this.ownerSource = ownerSource;
    }

    public String getStorageFormat() {
        return storageFormat;
    }

    public void setStorageFormat(String storageFormat) {
        this.storageFormat = storageFormat;
    }

    public String getLocationPath() {
        return locationPath;
    }

    public void setLocationPath(String locationPath) {
        this.locationPath = locationPath;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Long getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Long partitionCount) {
        this.partitionCount = partitionCount;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Column(name = "governance_score")
    private Double governanceScore;

    public Double getGovernanceScore() {
        return governanceScore;
    }

    public void setGovernanceScore(Double governanceScore) {
        this.governanceScore = governanceScore;
    }

    @Column(name = "sync_time")
    private LocalDateTime syncTime;

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
