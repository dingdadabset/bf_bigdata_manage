package com.dga.access.dto.openapi;

import java.util.List;

public class AuthzGrantRequest {

    private String clusterCode;
    private String username;
    private List<AuthzGrantItem> grants;

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<AuthzGrantItem> getGrants() {
        return grants;
    }

    public void setGrants(List<AuthzGrantItem> grants) {
        this.grants = grants;
    }
}
