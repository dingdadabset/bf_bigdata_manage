package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_governance_ai_advice",
        indexes = {
                @Index(name = "idx_governance_ai_advice_issue", columnList = "issue_id"),
                @Index(name = "idx_governance_ai_advice_action", columnList = "suggested_action")
        })
public class AccessGovernanceAiAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "risk_reason", length = 2000)
    private String riskReason;

    @Column(name = "suggested_action", length = 50)
    private String suggestedAction;

    @Column(name = "suggested_owner", length = 100)
    private String suggestedOwner;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "approval_required")
    private Boolean approvalRequired;

    @Lob
    @Column(name = "advice_json")
    private String adviceJson;

    @Column(name = "prompt_hash", length = 100)
    private String promptHash;

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
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getSuggestedOwner() { return suggestedOwner; }
    public void setSuggestedOwner(String suggestedOwner) { this.suggestedOwner = suggestedOwner; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Boolean getApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(Boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public String getAdviceJson() { return adviceJson; }
    public void setAdviceJson(String adviceJson) { this.adviceJson = adviceJson; }
    public String getPromptHash() { return promptHash; }
    public void setPromptHash(String promptHash) { this.promptHash = promptHash; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
