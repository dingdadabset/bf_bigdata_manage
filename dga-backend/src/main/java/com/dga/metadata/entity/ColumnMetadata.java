package com.dga.metadata.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "meta_column_info")
public class ColumnMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Column(name = "column_type", nullable = false)
    private String columnType; // e.g., STRING, INT, BIGINT

    @Column(name = "data_type")
    private String dataType;

    @PrePersist
    @PreUpdate
    public void syncDataType() {
        if (this.dataType == null) {
            this.dataType = this.columnType;
        }
    }

    @Column(name = "column_comment")
    private String comment;

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey;

    @Column(name = "security_level")
    private String securityLevel; // L1 (Public), L2 (Internal), L3 (Confidential), L4 (Strict)
}
