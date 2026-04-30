package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_activity_evidence",
        indexes = {
                @Index(name = "idx_access_activity_user", columnList = "username, cluster_name, source_system"),
                @Index(name = "idx_access_activity_source", columnList = "source_system, collected_at"),
                @Index(name = "idx_access_activity_key", columnList = "evidence_key")
        })
public class AccessActivityEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_key", nullable = false, length = 255)
    private String evidenceKey;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "cluster_code", length = 100)
    private String clusterCode;

    @Column(name = "cluster_name", length = 255)
    private String clusterName;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "evidence", length = 1000)
    private String evidence;

    @Column(name = "confidence", length = 20)
    private String confidence;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @PrePersist
    public void prePersist() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
        if (status == null || status.trim().isEmpty()) {
            status = lastActiveAt == null ? "PRESENT" : "OBSERVED";
        }
    }

    @PreUpdate
    public void preUpdate() {
        collectedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEvidenceKey() {
        return evidenceKey;
    }

    public void setEvidenceKey(String evidenceKey) {
        this.evidenceKey = evidenceKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}
