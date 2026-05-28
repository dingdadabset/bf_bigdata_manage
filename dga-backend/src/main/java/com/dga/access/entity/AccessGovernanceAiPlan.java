package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_governance_ai_plan")
public class AccessGovernanceAiPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster", length = 100)
    private String cluster;

    @Column(name = "scope_type", length = 50)
    private String scopeType;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Lob
    @Column(name = "plan_json")
    private String planJson;

    @Column(name = "status", length = 50)
    private String status = "DRAFT";

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
