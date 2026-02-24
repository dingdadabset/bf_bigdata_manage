package com.dga.metadata.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dga_table_score")
public class TableScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "storage_score", precision = 5, scale = 2)
    private BigDecimal storageScore;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "security_score", precision = 5, scale = 2)
    private BigDecimal securityScore;

    @Column(name = "cost_score", precision = 5, scale = 2)
    private BigDecimal costScore;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public BigDecimal getStorageScore() { return storageScore; }
    public void setStorageScore(BigDecimal storageScore) { this.storageScore = storageScore; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public BigDecimal getSecurityScore() { return securityScore; }
    public void setSecurityScore(BigDecimal securityScore) { this.securityScore = securityScore; }
    public BigDecimal getCostScore() { return costScore; }
    public void setCostScore(BigDecimal costScore) { this.costScore = costScore; }
}
