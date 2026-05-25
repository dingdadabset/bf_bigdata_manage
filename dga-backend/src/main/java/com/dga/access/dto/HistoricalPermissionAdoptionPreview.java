package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class HistoricalPermissionAdoptionPreview {
    private String username;
    private String cluster;
    private String authBackend;
    private String roleCode;
    private String subjectType;
    private String subjectName;
    private String reconciliationStatus;
    private Summary summary = new Summary();
    private List<Item> items = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String snapshotHash;

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
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public String getSnapshotHash() { return snapshotHash; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }

    public static class Summary {
        private int rolePermissionCount;
        private int liveDirectCount;
        private int liveUserRoleCount;
        private int liveGroupInheritedCount;
        private int recordedCount;
        private int matchedDirectCount;
        private int matchedGroupInheritedCount;
        private int roleMissingLiveCount;
        private int liveExtraDirectCount;
        private int liveExtraGroupInheritedCount;
        private int alreadyRecordedCount;
        private int adoptableCount;
        private int blockedGroupInheritedCount;

        public int getRolePermissionCount() { return rolePermissionCount; }
        public void setRolePermissionCount(int rolePermissionCount) { this.rolePermissionCount = rolePermissionCount; }
        public int getLiveDirectCount() { return liveDirectCount; }
        public void setLiveDirectCount(int liveDirectCount) { this.liveDirectCount = liveDirectCount; }
        public int getLiveUserRoleCount() { return liveUserRoleCount; }
        public void setLiveUserRoleCount(int liveUserRoleCount) { this.liveUserRoleCount = liveUserRoleCount; }
        public int getLiveGroupInheritedCount() { return liveGroupInheritedCount; }
        public void setLiveGroupInheritedCount(int liveGroupInheritedCount) { this.liveGroupInheritedCount = liveGroupInheritedCount; }
        public int getRecordedCount() { return recordedCount; }
        public void setRecordedCount(int recordedCount) { this.recordedCount = recordedCount; }
        public int getMatchedDirectCount() { return matchedDirectCount; }
        public void setMatchedDirectCount(int matchedDirectCount) { this.matchedDirectCount = matchedDirectCount; }
        public int getMatchedGroupInheritedCount() { return matchedGroupInheritedCount; }
        public void setMatchedGroupInheritedCount(int matchedGroupInheritedCount) { this.matchedGroupInheritedCount = matchedGroupInheritedCount; }
        public int getRoleMissingLiveCount() { return roleMissingLiveCount; }
        public void setRoleMissingLiveCount(int roleMissingLiveCount) { this.roleMissingLiveCount = roleMissingLiveCount; }
        public int getLiveExtraDirectCount() { return liveExtraDirectCount; }
        public void setLiveExtraDirectCount(int liveExtraDirectCount) { this.liveExtraDirectCount = liveExtraDirectCount; }
        public int getLiveExtraGroupInheritedCount() { return liveExtraGroupInheritedCount; }
        public void setLiveExtraGroupInheritedCount(int liveExtraGroupInheritedCount) { this.liveExtraGroupInheritedCount = liveExtraGroupInheritedCount; }
        public int getAlreadyRecordedCount() { return alreadyRecordedCount; }
        public void setAlreadyRecordedCount(int alreadyRecordedCount) { this.alreadyRecordedCount = alreadyRecordedCount; }
        public int getAdoptableCount() { return adoptableCount; }
        public void setAdoptableCount(int adoptableCount) { this.adoptableCount = adoptableCount; }
        public int getBlockedGroupInheritedCount() { return blockedGroupInheritedCount; }
        public void setBlockedGroupInheritedCount(int blockedGroupInheritedCount) { this.blockedGroupInheritedCount = blockedGroupInheritedCount; }
    }

    public static class Item {
        private String key;
        private String resourceType;
        private String databaseName;
        private String tableName;
        private String permission;
        private String authBackend;
        private String verificationStatus;
        private String adoptionStatus;
        private boolean adoptable;
        private boolean requiresGroupInheritedAcknowledgement;
        private String source;
        private String sourceRole;
        private String sourceGroup;
        private String grantText;
        private String warning;
        private boolean reverseBindable;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }
        public String getAuthBackend() { return authBackend; }
        public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public String getAdoptionStatus() { return adoptionStatus; }
        public void setAdoptionStatus(String adoptionStatus) { this.adoptionStatus = adoptionStatus; }
        public boolean isAdoptable() { return adoptable; }
        public void setAdoptable(boolean adoptable) { this.adoptable = adoptable; }
        public boolean isRequiresGroupInheritedAcknowledgement() { return requiresGroupInheritedAcknowledgement; }
        public void setRequiresGroupInheritedAcknowledgement(boolean requiresGroupInheritedAcknowledgement) { this.requiresGroupInheritedAcknowledgement = requiresGroupInheritedAcknowledgement; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSourceRole() { return sourceRole; }
        public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }
        public String getSourceGroup() { return sourceGroup; }
        public void setSourceGroup(String sourceGroup) { this.sourceGroup = sourceGroup; }
        public String getGrantText() { return grantText; }
        public void setGrantText(String grantText) { this.grantText = grantText; }
        public String getWarning() { return warning; }
        public void setWarning(String warning) { this.warning = warning; }
        public boolean isReverseBindable() { return reverseBindable; }
        public void setReverseBindable(boolean reverseBindable) { this.reverseBindable = reverseBindable; }
    }
}
