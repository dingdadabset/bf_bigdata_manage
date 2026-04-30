package com.dga.access.dto;

import java.util.List;

public class GovernanceIssueActionRequest {

    private String owner;
    private String ownerDisplayName;
    private String ownerEmail;
    private List<String> collaboratorOwners;
    private String assignee;
    private String comment;
    private Integer reviewDays;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public List<String> getCollaboratorOwners() {
        return collaboratorOwners;
    }

    public void setCollaboratorOwners(List<String> collaboratorOwners) {
        this.collaboratorOwners = collaboratorOwners;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getReviewDays() {
        return reviewDays;
    }

    public void setReviewDays(Integer reviewDays) {
        this.reviewDays = reviewDays;
    }
}
