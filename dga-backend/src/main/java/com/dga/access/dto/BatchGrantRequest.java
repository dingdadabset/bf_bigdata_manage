package com.dga.access.dto;

import java.util.List;

public class BatchGrantRequest {

    private String username;
    private String permission;
    private String level;
    private String cluster;
    private List<String> databases;
    private List<TableGrant> tables;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<TableGrant> getTables() {
        return tables;
    }

    public void setTables(List<TableGrant> tables) {
        this.tables = tables;
    }
}
