package com.dga.access.dto;

import java.util.List;

public class BatchGrantRequest {

    private String username;
    private String permission;
    private List<String> permissions;
    private String level;
    private String cluster;
    private String authBackend;
    private String grantMode;
    private String roleCode;
    private String subjectType;
    private String subjectName;
    private String exceptionReason;
    private String ticketNo;
    private String approver;
    private String expiresAt;
    private String riskLevel;
    private Boolean roleSubsetMode;
    private List<RolePermissionSelection> rolePermissions;
    private List<String> databases;
    private List<TableGrant> tables;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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

    public String getGrantMode() {
        return grantMode;
    }

    public void setGrantMode(String grantMode) {
        this.grantMode = grantMode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getExceptionReason() {
        return exceptionReason;
    }

    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getRoleSubsetMode() {
        return roleSubsetMode;
    }

    public void setRoleSubsetMode(Boolean roleSubsetMode) {
        this.roleSubsetMode = roleSubsetMode;
    }

    public List<RolePermissionSelection> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(List<RolePermissionSelection> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<TableGrant> getTables() {
        return tables;
    }

    public void setTables(List<TableGrant> tables) {
        this.tables = tables;
    }

    public static class RolePermissionSelection {
        private String resourceType;
        private String databaseName;
        private String tableName;
        private String permission;
        private String authBackend;

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public String getAuthBackend() {
            return authBackend;
        }

        public void setAuthBackend(String authBackend) {
            this.authBackend = authBackend;
        }
    }
}
