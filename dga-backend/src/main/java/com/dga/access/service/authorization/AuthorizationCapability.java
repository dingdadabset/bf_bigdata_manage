package com.dga.access.service.authorization;

import java.util.ArrayList;
import java.util.List;

public class AuthorizationCapability {
    private String cluster;
    private String clusterCode;
    private String clusterName;
    private String engineType;
    private String authBackend;
    private String endpointType;
    private String endpointUrl;
    private List<String> principalTypes = new ArrayList<>();
    private List<String> resourceTypes = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private boolean requiresLdap;
    private String status;
    private List<String> warnings = new ArrayList<>();

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public List<String> getPrincipalTypes() {
        return principalTypes;
    }

    public void setPrincipalTypes(List<String> principalTypes) {
        this.principalTypes = principalTypes;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean isRequiresLdap() {
        return requiresLdap;
    }

    public void setRequiresLdap(boolean requiresLdap) {
        this.requiresLdap = requiresLdap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
