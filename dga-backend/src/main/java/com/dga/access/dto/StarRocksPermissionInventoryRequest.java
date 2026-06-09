package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class StarRocksPermissionInventoryRequest {
    private String cluster;
    private String authBackend;
    private List<String> usernames = new ArrayList<>();
    private String keyword;
    private Integer maxUsers = 50;
    private Boolean includeRecordedGrants = Boolean.TRUE;

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

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Boolean getIncludeRecordedGrants() {
        return includeRecordedGrants;
    }

    public void setIncludeRecordedGrants(Boolean includeRecordedGrants) {
        this.includeRecordedGrants = includeRecordedGrants;
    }
}
