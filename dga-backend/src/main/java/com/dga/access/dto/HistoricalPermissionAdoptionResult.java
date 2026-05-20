package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class HistoricalPermissionAdoptionResult {
    private String username;
    private String cluster;
    private String authBackend;
    private String roleCode;
    private String subjectType;
    private String subjectName;
    private String reconciliationStatus;
    private String assignmentStatus;
    private int requestedCount;
    private int adoptedCount;
    private int skippedExistingCount;
    private int blockedCount;
    private List<Item> items = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) { this.assignmentStatus = assignmentStatus; }
    public int getRequestedCount() { return requestedCount; }
    public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }
    public int getAdoptedCount() { return adoptedCount; }
    public void setAdoptedCount(int adoptedCount) { this.adoptedCount = adoptedCount; }
    public int getSkippedExistingCount() { return skippedExistingCount; }
    public void setSkippedExistingCount(int skippedExistingCount) { this.skippedExistingCount = skippedExistingCount; }
    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public static class Item {
        private String key;
        private String action;
        private String message;
        private Long recordId;
        private String source;
        private String sourceRole;
        private String sourceGroup;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSourceRole() { return sourceRole; }
        public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }
        public String getSourceGroup() { return sourceGroup; }
        public void setSourceGroup(String sourceGroup) { this.sourceGroup = sourceGroup; }
    }
}
