package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthRoleImportResult {
    private int total;
    private int created;
    private int updated;
    private int skipped;
    private int failed;
    private List<Item> items = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }
    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private String roleCode;
        private String action;
        private String status;
        private String message;
        private int permissionCount;
        private int assignmentCount;

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getPermissionCount() { return permissionCount; }
        public void setPermissionCount(int permissionCount) { this.permissionCount = permissionCount; }
        public int getAssignmentCount() { return assignmentCount; }
        public void setAssignmentCount(int assignmentCount) { this.assignmentCount = assignmentCount; }
    }
}
