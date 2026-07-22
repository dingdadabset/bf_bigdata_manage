package com.dga.access.dto;

public class CreateUserRequest {

    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String creationStrategy; // "LDAP" (default) or "IPA_SSH" or "IPA_HTTP"
    private String ipaHost; // optional override
    private String cluster;
    private String userType; // INTERNAL, OUTSOURCER, TEMPORARY, SERVICE
    private String accountMode; // LDAP_ONLY or POSIX_ACCOUNT
    private java.time.LocalDateTime expiresAt;
    private Long gidNumber;
    private String groupName;
    private String groupStrategy;
    private String newGroupName;
    private String newGroupDescription;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCreationStrategy() {
        return creationStrategy;
    }

    public void setCreationStrategy(String creationStrategy) {
        this.creationStrategy = creationStrategy;
    }

    public String getIpaHost() {
        return ipaHost;
    }

    public void setIpaHost(String ipaHost) {
        this.ipaHost = ipaHost;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getAccountMode() {
        return accountMode;
    }

    public void setAccountMode(String accountMode) {
        this.accountMode = accountMode;
    }

    public java.time.LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(java.time.LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getGidNumber() {
        return gidNumber;
    }

    public void setGidNumber(Long gidNumber) {
        this.gidNumber = gidNumber;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupStrategy() {
        return groupStrategy;
    }

    public void setGroupStrategy(String groupStrategy) {
        this.groupStrategy = groupStrategy;
    }

    public String getNewGroupName() {
        return newGroupName;
    }

    public void setNewGroupName(String newGroupName) {
        this.newGroupName = newGroupName;
    }

    public String getNewGroupDescription() {
        return newGroupDescription;
    }

    public void setNewGroupDescription(String newGroupDescription) {
        this.newGroupDescription = newGroupDescription;
    }
}
