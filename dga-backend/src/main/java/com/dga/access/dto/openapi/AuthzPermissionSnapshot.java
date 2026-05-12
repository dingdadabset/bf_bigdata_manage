package com.dga.access.dto.openapi;

import java.util.List;

public class AuthzPermissionSnapshot {

    private String clusterCode;
    private String username;
    private String engineType;
    private String authBackend;
    private List<AuthzGrantView> grants;

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

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(String authBackend) {
        this.authBackend = authBackend;
    }

    public List<AuthzGrantView> getGrants() {
        return grants;
    }

    public void setGrants(List<AuthzGrantView> grants) {
        this.grants = grants;
    }
}
