package com.dga.access.dto;

public class BatchRoleAssignmentItem {
    private String input;
    private String username;
    private String action;
    private String status;
    private String message;
    private boolean alreadyAssigned;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAlreadyAssigned() {
        return alreadyAssigned;
    }

    public void setAlreadyAssigned(boolean alreadyAssigned) {
        this.alreadyAssigned = alreadyAssigned;
    }
}
