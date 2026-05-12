package com.dga.lineage.service;

import java.util.ArrayList;
import java.util.List;

public class ParsedSchedulerTaskContext {

    private String projectName;
    private String workflowName;
    private String taskName;
    private String taskKey;
    private String jobPath;
    private String commandText;
    private List<String> inputTables = new ArrayList<>();
    private List<String> outputTables = new ArrayList<>();
    private String parseStatus = "SUCCESS";
    private String parseMessage;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getJobPath() {
        return jobPath;
    }

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }

    public String getCommandText() {
        return commandText;
    }

    public void setCommandText(String commandText) {
        this.commandText = commandText;
    }

    public List<String> getInputTables() {
        return inputTables;
    }

    public void setInputTables(List<String> inputTables) {
        this.inputTables = inputTables == null ? new ArrayList<>() : inputTables;
    }

    public List<String> getOutputTables() {
        return outputTables;
    }

    public void setOutputTables(List<String> outputTables) {
        this.outputTables = outputTables == null ? new ArrayList<>() : outputTables;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseMessage() {
        return parseMessage;
    }

    public void setParseMessage(String parseMessage) {
        this.parseMessage = parseMessage;
    }
}
