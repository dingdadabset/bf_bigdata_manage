package com.dga.access.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffboardingRevocationTaskView {

    private Long taskId;
    private String taskNo;
    private String username;
    private String cluster;
    private LocalDate departureDate;
    private LocalDateTime scheduledAt;
    private String managerContact;
    private String securityContact;
    private String reason;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String confirmationNo;
    private String archiveName;
    private String confirmationText;
    private String message;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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
