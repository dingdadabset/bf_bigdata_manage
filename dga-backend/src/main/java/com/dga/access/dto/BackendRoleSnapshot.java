package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class BackendRoleSnapshot {
    private String roleCode;
    private String roleName;
    private String cluster;
    private String authBackend;
    private String engineType;
    private boolean localExists;
    private String localStatus;
    private List<String> groupNames = new ArrayList<>();
    private List<BackendRolePermissionSnapshot> permissions = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }
    public boolean isLocalExists() { return localExists; }
    public void setLocalExists(boolean localExists) { this.localExists = localExists; }
    public String getLocalStatus() { return localStatus; }
    public void setLocalStatus(String localStatus) { this.localStatus = localStatus; }
    public List<String> getGroupNames() { return groupNames; }
    public void setGroupNames(List<String> groupNames) { this.groupNames = groupNames; }
    public List<BackendRolePermissionSnapshot> getPermissions() { return permissions; }
    public void setPermissions(List<BackendRolePermissionSnapshot> permissions) { this.permissions = permissions; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
