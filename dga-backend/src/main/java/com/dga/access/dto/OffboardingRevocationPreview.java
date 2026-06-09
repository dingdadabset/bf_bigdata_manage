package com.dga.access.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OffboardingRevocationPreview {

    private String username;
    private String cluster;
    private LocalDate departureDate;
    private boolean executable;
    private String revocationFlow;
    private String engineLabel;
    private boolean nativeSqlFlow;
    private boolean userResolved;
    private String displayName;
    private String email;
    private String creationStrategy;
    private String userType;
    private Boolean ldapLocked;
    private boolean protectedUser;
    private boolean activeTaskExists;
    private Long activeTaskId;
    private String activeTaskNo;
    private String activeTaskStatus;
    private Boolean accountExists;
    private String accountCheckStatus;
    private String accountCheckMessage;
    private Integer livePermissionCount;
    private String livePermissionMessage;
    private long recordedPermissionCount;
    private List<String> notificationRecipients = new ArrayList<>();
    private List<String> blockers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<OffboardingRevocationResult.StepResult> plannedSteps = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public String getRevocationFlow() {
        return revocationFlow;
    }

    public void setRevocationFlow(String revocationFlow) {
        this.revocationFlow = revocationFlow;
    }

    public String getEngineLabel() {
        return engineLabel;
    }

    public void setEngineLabel(String engineLabel) {
        this.engineLabel = engineLabel;
    }

    public boolean isNativeSqlFlow() {
        return nativeSqlFlow;
    }

    public void setNativeSqlFlow(boolean nativeSqlFlow) {
        this.nativeSqlFlow = nativeSqlFlow;
    }

    public boolean isUserResolved() {
        return userResolved;
    }

    public void setUserResolved(boolean userResolved) {
        this.userResolved = userResolved;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreationStrategy() {
        return creationStrategy;
    }

    public void setCreationStrategy(String creationStrategy) {
        this.creationStrategy = creationStrategy;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Boolean getLdapLocked() {
        return ldapLocked;
    }

    public void setLdapLocked(Boolean ldapLocked) {
        this.ldapLocked = ldapLocked;
    }

    public boolean isProtectedUser() {
        return protectedUser;
    }

    public void setProtectedUser(boolean protectedUser) {
        this.protectedUser = protectedUser;
    }

    public boolean isActiveTaskExists() {
        return activeTaskExists;
    }

    public void setActiveTaskExists(boolean activeTaskExists) {
        this.activeTaskExists = activeTaskExists;
    }

    public Long getActiveTaskId() {
        return activeTaskId;
    }

    public void setActiveTaskId(Long activeTaskId) {
        this.activeTaskId = activeTaskId;
    }

    public String getActiveTaskNo() {
        return activeTaskNo;
    }

    public void setActiveTaskNo(String activeTaskNo) {
        this.activeTaskNo = activeTaskNo;
    }

    public String getActiveTaskStatus() {
        return activeTaskStatus;
    }

    public void setActiveTaskStatus(String activeTaskStatus) {
        this.activeTaskStatus = activeTaskStatus;
    }

    public Boolean getAccountExists() {
        return accountExists;
    }

    public void setAccountExists(Boolean accountExists) {
        this.accountExists = accountExists;
    }

    public String getAccountCheckStatus() {
        return accountCheckStatus;
    }

    public void setAccountCheckStatus(String accountCheckStatus) {
        this.accountCheckStatus = accountCheckStatus;
    }

    public String getAccountCheckMessage() {
        return accountCheckMessage;
    }

    public void setAccountCheckMessage(String accountCheckMessage) {
        this.accountCheckMessage = accountCheckMessage;
    }

    public Integer getLivePermissionCount() {
        return livePermissionCount;
    }

    public void setLivePermissionCount(Integer livePermissionCount) {
        this.livePermissionCount = livePermissionCount;
    }

    public String getLivePermissionMessage() {
        return livePermissionMessage;
    }

    public void setLivePermissionMessage(String livePermissionMessage) {
        this.livePermissionMessage = livePermissionMessage;
    }

    public long getRecordedPermissionCount() {
        return recordedPermissionCount;
    }

    public void setRecordedPermissionCount(long recordedPermissionCount) {
        this.recordedPermissionCount = recordedPermissionCount;
    }

    public List<String> getNotificationRecipients() {
        return notificationRecipients;
    }

    public void setNotificationRecipients(List<String> notificationRecipients) {
        this.notificationRecipients = notificationRecipients;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<String> blockers) {
        this.blockers = blockers;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<OffboardingRevocationResult.StepResult> getPlannedSteps() {
        return plannedSteps;
    }

    public void setPlannedSteps(List<OffboardingRevocationResult.StepResult> plannedSteps) {
        this.plannedSteps = plannedSteps;
    }

    public void addBlocker(String blocker) {
        this.blockers.add(blocker);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
