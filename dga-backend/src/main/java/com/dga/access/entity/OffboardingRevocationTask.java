package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_offboarding_revocation_task",
        indexes = {
                @Index(name = "idx_offboarding_task_due", columnList = "status, scheduled_at"),
                @Index(name = "idx_offboarding_task_user", columnList = "username, cluster_name, status")
        })
public class OffboardingRevocationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, unique = true, length = 80)
    private String taskNo;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "cluster_name", nullable = false, length = 100)
    private String cluster;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "manager_contact")
    private String managerContact;

    @Column(name = "security_contact")
    private String securityContact;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "confirmation_no", length = 100)
    private String confirmationNo;

    @Column(name = "archive_name", length = 200)
    private String archiveName;

    @Lob
    @Column(name = "confirmation_text")
    private String confirmationText;

    @Column(name = "message", length = 1000)
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getManagerContact() {
        return managerContact;
    }

    public void setManagerContact(String managerContact) {
        this.managerContact = managerContact;
    }

    public String getSecurityContact() {
        return securityContact;
    }

    public void setSecurityContact(String securityContact) {
        this.securityContact = securityContact;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public void setConfirmationNo(String confirmationNo) {
        this.confirmationNo = confirmationNo;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getConfirmationText() {
        return confirmationText;
    }

    public void setConfirmationText(String confirmationText) {
        this.confirmationText = confirmationText;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
