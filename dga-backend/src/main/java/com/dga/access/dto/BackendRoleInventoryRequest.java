package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class BackendRoleInventoryRequest {
    private String cluster;
    private String authBackend;
    private String keyword;
    private List<String> roleCodes = new ArrayList<>();
    private boolean includePermissions = true;
    private boolean includeAssignments = true;

    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
    public boolean isIncludePermissions() { return includePermissions; }
    public void setIncludePermissions(boolean includePermissions) { this.includePermissions = includePermissions; }
    public boolean isIncludeAssignments() { return includeAssignments; }
    public void setIncludeAssignments(boolean includeAssignments) { this.includeAssignments = includeAssignments; }
}
