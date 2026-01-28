package com.dga.metadata.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_table_metadata")
public class TableMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "db_name", nullable = false)
    private String dbName;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "owner")
    private String owner;

    @Column(name = "storage_format")
    private String storageFormat; // e.g., ORC, PARQUET, TEXTFILE

    @Column(name = "location_path")
    private String locationPath;

    @Column(name = "total_size")
    private Long totalSize; // in bytes

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "last_access_time")
    private LocalDateTime lastAccessTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
