package com.dga.lineage.service;

public class ParsedLineageEdge {

    private String sourceDb;
    private String sourceTable;
    private String targetDb;
    private String targetTable;
    private String sourceProject;
    private String sourceWorkflow;
    private String sourceTask;
    private String sourceTaskKey;
    private String sql;
    private String sqlHash;

    public String uniqueKey() {
        return sourceDb + "." + sourceTable + "->" + targetDb + "." + targetTable
                + "|" + sourceTaskKey + "|" + sqlHash;
    }

    public String getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(String sourceDb) {
        this.sourceDb = sourceDb;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetDb() {
        return targetDb;
    }

    public void setTargetDb(String targetDb) {
        this.targetDb = targetDb;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getSourceProject() {
        return sourceProject;
    }

    public void setSourceProject(String sourceProject) {
        this.sourceProject = sourceProject;
    }

    public String getSourceWorkflow() {
        return sourceWorkflow;
    }

    public void setSourceWorkflow(String sourceWorkflow) {
        this.sourceWorkflow = sourceWorkflow;
    }

    public String getSourceTask() {
        return sourceTask;
    }

    public void setSourceTask(String sourceTask) {
        this.sourceTask = sourceTask;
    }

    public String getSourceTaskKey() {
        return sourceTaskKey;
    }

    public void setSourceTaskKey(String sourceTaskKey) {
        this.sourceTaskKey = sourceTaskKey;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSqlHash() {
        return sqlHash;
    }

    public void setSqlHash(String sqlHash) {
        this.sqlHash = sqlHash;
    }
}
