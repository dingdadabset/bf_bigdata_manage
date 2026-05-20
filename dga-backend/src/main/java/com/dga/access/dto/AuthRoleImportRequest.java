package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthRoleImportRequest {
    private String cluster;
    private String authBackend;
    private List<String> roleCodes = new ArrayList<>();
    private boolean importPermissions = true;
    private boolean importAssignments = true;
    private String operator;

    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
    public boolean isImportPermissions() { return importPermissions; }
    public void setImportPermissions(boolean importPermissions) { this.importPermissions = importPermissions; }
    public boolean isImportAssignments() { return importAssignments; }
    public void setImportAssignments(boolean importAssignments) { this.importAssignments = importAssignments; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
