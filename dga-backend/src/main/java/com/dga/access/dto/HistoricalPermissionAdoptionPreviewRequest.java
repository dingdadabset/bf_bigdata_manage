package com.dga.access.dto;

public class HistoricalPermissionAdoptionPreviewRequest {
    private String username;
    private String cluster;
    private String authBackend;
    private String subjectType;
    private String subjectName;
    private Boolean includeGroupInherited;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Boolean getIncludeGroupInherited() { return includeGroupInherited; }
    public void setIncludeGroupInherited(Boolean includeGroupInherited) { this.includeGroupInherited = includeGroupInherited; }
}
