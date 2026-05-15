package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchRoleAssignmentResult {
    private String roleCode;
    private String roleName;
    private String cluster;
    private String authBackend;
    private boolean dryRun;
    private int total;
    private int validCount;
    private int duplicateCount;
    private int alreadyBoundCount;
    private int willBindCount;
    private int successCount;
    private int failedCount;
    private int pendingGroupMappingCount;
    private int skippedCount;
    private List<BatchRoleAssignmentItem> items = new ArrayList<>();

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(String authBackend) {
        this.authBackend = authBackend;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getAlreadyBoundCount() {
        return alreadyBoundCount;
    }

    public void setAlreadyBoundCount(int alreadyBoundCount) {
        this.alreadyBoundCount = alreadyBoundCount;
    }

    public int getWillBindCount() {
        return willBindCount;
    }

    public void setWillBindCount(int willBindCount) {
        this.willBindCount = willBindCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getPendingGroupMappingCount() {
        return pendingGroupMappingCount;
    }

    public void setPendingGroupMappingCount(int pendingGroupMappingCount) {
        this.pendingGroupMappingCount = pendingGroupMappingCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<BatchRoleAssignmentItem> getItems() {
        return items;
    }

    public void setItems(List<BatchRoleAssignmentItem> items) {
        this.items = items;
    }
}
