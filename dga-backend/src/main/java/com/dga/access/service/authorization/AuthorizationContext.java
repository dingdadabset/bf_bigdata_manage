package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;

import java.util.List;

public class AuthorizationContext {
    private Cluster cluster;
    private String clusterIdentifier;
    private List<ClusterEndpoint> endpoints;

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getClusterIdentifier() {
        return clusterIdentifier;
    }

    public void setClusterIdentifier(String clusterIdentifier) {
        this.clusterIdentifier = clusterIdentifier;
    }

    public List<ClusterEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<ClusterEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getClusterCodeOrName() {
        if (cluster != null && cluster.getClusterCode() != null) {
            return cluster.getClusterCode();
        }
        if (cluster != null && cluster.getClusterName() != null) {
            return cluster.getClusterName();
        }
        return clusterIdentifier;
    }
}
