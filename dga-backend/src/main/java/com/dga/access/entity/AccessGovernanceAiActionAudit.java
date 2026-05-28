package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_governance_ai_action_audit")
public class AccessGovernanceAiActionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "advice_id")
    private Long adviceId;

    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "suggested_action", length = 50)
    private String suggestedAction;

    @Column(name = "executed_action", length = 50)
    private String executedAction;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "result_status", length = 50)
    private String resultStatus;

    @Column(name = "result_message", length = 2000)
    private String resultMessage;

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
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getAdviceId() { return adviceId; }
    public void setAdviceId(Long adviceId) { this.adviceId = adviceId; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getExecutedAction() { return executedAction; }
    public void setExecutedAction(String executedAction) { this.executedAction = executedAction; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
