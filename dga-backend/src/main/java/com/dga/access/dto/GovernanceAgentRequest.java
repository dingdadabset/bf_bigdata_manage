package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class GovernanceAgentRequest {

    private Long issueId;
    private List<Long> issueIds = new ArrayList<>();
    private String scope;
    private String cluster;
    private String status;
    private List<String> issueTypes = new ArrayList<>();
    private String startDate;
    private String endDate;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public List<Long> getIssueIds() { return issueIds; }
    public void setIssueIds(List<Long> issueIds) { this.issueIds = issueIds == null ? new ArrayList<>() : issueIds; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getIssueTypes() { return issueTypes; }
    public void setIssueTypes(List<String> issueTypes) { this.issueTypes = issueTypes == null ? new ArrayList<>() : issueTypes; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
