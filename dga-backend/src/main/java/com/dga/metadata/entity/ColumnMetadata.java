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

    @Column(name = "column_type", nullable = false)
    private String columnType; // e.g., STRING, INT, BIGINT

    @Column(name = "column_comment")
    private String comment;

    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey;

    @Column(name = "security_level")
    private String securityLevel; // L1 (Public), L2 (Internal), L3 (Confidential), L4 (Strict)
}
