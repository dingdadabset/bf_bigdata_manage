package com.dga.metadata.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_metric_definition")
public class MetricDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "metric_code", nullable = false)
    private String metricCode;

    @Column(name = "business_definition", columnDefinition = "TEXT")
    private String businessDefinition;

    @Column(name = "calculation_logic", columnDefinition = "TEXT")
    private String calculationLogic;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "owner")
    private String owner;

    @Column(name = "status")
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
