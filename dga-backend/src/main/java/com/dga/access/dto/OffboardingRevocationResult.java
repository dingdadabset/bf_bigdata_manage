package com.dga.access.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffboardingRevocationResult {

    private String confirmationNo;
    private Long taskId;
    private String taskNo;
    private String taskStatus;
    private String username;
    private String cluster;
    private String revocationFlow;
    private LocalDate departureDate;
    private LocalDateTime scheduledAt;
    private String archiveName;
    private String confirmationText;
    private LocalDateTime executedAt;
    private List<String> notificationRecipients = new ArrayList<>();
    private List<StepResult> steps = new ArrayList<>();

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public void setConfirmationNo(String confirmationNo) {
        this.confirmationNo = confirmationNo;
    }

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

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
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

    public String getRevocationFlow() {
        return revocationFlow;
    }

    public void setRevocationFlow(String revocationFlow) {
        this.revocationFlow = revocationFlow;
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

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public List<String> getNotificationRecipients() {
        return notificationRecipients;
    }

    public void setNotificationRecipients(List<String> notificationRecipients) {
        this.notificationRecipients = notificationRecipients;
    }

    public List<StepResult> getSteps() {
        return steps;
    }

    public void setSteps(List<StepResult> steps) {
        this.steps = steps;
    }

    public void addStep(StepResult step) {
        this.steps.add(step);
    }

    public static class StepResult {
        private String key;
        private String title;
        private String status;
        private String message;

        public StepResult() {
        }

        public StepResult(String key, String title, String status, String message) {
            this.key = key;
            this.title = title;
            this.status = status;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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
    }
}
