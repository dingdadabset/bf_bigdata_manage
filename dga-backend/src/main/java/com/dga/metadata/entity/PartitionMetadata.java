package com.dga.metadata.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "meta_partition_info")
public class PartitionMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "datasource_id")
    private Long dataSourceId;

    @Column(name = "cluster_code")
    private String clusterCode;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "partition_name", nullable = false, length = 500)
    private String partitionName;

    @Column(name = "partition_spec", length = 1000)
    private String partitionSpec;

    @Column(name = "hdfs_path", length = 1000)
    private String locationPath;

    @Column(name = "storage_format")
    private String storageFormat;

    @Column(name = "table_size")
    private Long totalSize;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "last_modify_time")
    private LocalDateTime lastModifyTime;

    @Column(name = "sync_time")
    private LocalDateTime syncTime;
}
