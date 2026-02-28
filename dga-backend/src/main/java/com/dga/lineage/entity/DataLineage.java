package com.dga.lineage.entity;

import com.dga.metadata.entity.TableMetadata;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_data_lineage")
@EntityListeners(AuditingEntityListener.class)
public class DataLineage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_table_id", nullable = false)
    private Long sourceTableId;

    @Column(name = "target_table_id", nullable = false)
    private Long targetTableId;

    @Column(name = "lineage_type", nullable = false)
    private String lineageType; // ETL, VIEW, COPY, DERIVED

    @Column(name = "transformation_logic")
    private String transformationLogic; // SQL or description

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceTableId() {
        return sourceTableId;
    }

    public void setSourceTableId(Long sourceTableId) {
        this.sourceTableId = sourceTableId;
    }

    public Long getTargetTableId() {
        return targetTableId;
    }

    public void setTargetTableId(Long targetTableId) {
        this.targetTableId = targetTableId;
    }

    public String getLineageType() {
        return lineageType;
    }

    public void setLineageType(String lineageType) {
        this.lineageType = lineageType;
    }

    public String getTransformationLogic() {
        return transformationLogic;
    }

    public void setTransformationLogic(String transformationLogic) {
        this.transformationLogic = transformationLogic;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
