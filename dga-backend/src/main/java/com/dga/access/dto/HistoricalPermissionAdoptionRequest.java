package com.dga.access.dto;

import java.util.ArrayList;
import java.util.List;

public class HistoricalPermissionAdoptionRequest {
    private String username;
    private String cluster;
    private String authBackend;
    private String subjectType;
    private String subjectName;
    private List<BatchGrantRequest.RolePermissionSelection> rolePermissions = new ArrayList<>();
    private String expectedSnapshotHash;
    private Boolean bindRoleLocalOnly;
    private Boolean adoptDirectUserPermissions;
    private Boolean adoptUserRolePermissions;
    private Boolean adoptGroupInheritedPermissions;
    private Boolean acknowledgeGroupInherited;
    private Boolean allowPartialAdoption;
    private Boolean allowSupersetExtrasUnmanaged;
    private Boolean allowRoleMissingBackendPermissions;
    private String adoptionReason;
    private String ticketNo;
    private String approver;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }
    public String getAuthBackend() { return authBackend; }
    public void setAuthBackend(String authBackend) { this.authBackend = authBackend; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public List<BatchGrantRequest.RolePermissionSelection> getRolePermissions() { return rolePermissions; }
    public void setRolePermissions(List<BatchGrantRequest.RolePermissionSelection> rolePermissions) { this.rolePermissions = rolePermissions; }
    public String getExpectedSnapshotHash() { return expectedSnapshotHash; }
    public void setExpectedSnapshotHash(String expectedSnapshotHash) { this.expectedSnapshotHash = expectedSnapshotHash; }
    public Boolean getBindRoleLocalOnly() { return bindRoleLocalOnly; }
    public void setBindRoleLocalOnly(Boolean bindRoleLocalOnly) { this.bindRoleLocalOnly = bindRoleLocalOnly; }
    public Boolean getAdoptDirectUserPermissions() { return adoptDirectUserPermissions; }
    public void setAdoptDirectUserPermissions(Boolean adoptDirectUserPermissions) { this.adoptDirectUserPermissions = adoptDirectUserPermissions; }
    public Boolean getAdoptUserRolePermissions() { return adoptUserRolePermissions; }
    public void setAdoptUserRolePermissions(Boolean adoptUserRolePermissions) { this.adoptUserRolePermissions = adoptUserRolePermissions; }
    public Boolean getAdoptGroupInheritedPermissions() { return adoptGroupInheritedPermissions; }
    public void setAdoptGroupInheritedPermissions(Boolean adoptGroupInheritedPermissions) { this.adoptGroupInheritedPermissions = adoptGroupInheritedPermissions; }
    public Boolean getAcknowledgeGroupInherited() { return acknowledgeGroupInherited; }
    public void setAcknowledgeGroupInherited(Boolean acknowledgeGroupInherited) { this.acknowledgeGroupInherited = acknowledgeGroupInherited; }
    public Boolean getAllowPartialAdoption() { return allowPartialAdoption; }
    public void setAllowPartialAdoption(Boolean allowPartialAdoption) { this.allowPartialAdoption = allowPartialAdoption; }
    public Boolean getAllowSupersetExtrasUnmanaged() { return allowSupersetExtrasUnmanaged; }
    public void setAllowSupersetExtrasUnmanaged(Boolean allowSupersetExtrasUnmanaged) { this.allowSupersetExtrasUnmanaged = allowSupersetExtrasUnmanaged; }
    public Boolean getAllowRoleMissingBackendPermissions() { return allowRoleMissingBackendPermissions; }
    public void setAllowRoleMissingBackendPermissions(Boolean allowRoleMissingBackendPermissions) { this.allowRoleMissingBackendPermissions = allowRoleMissingBackendPermissions; }
    public String getAdoptionReason() { return adoptionReason; }
    public void setAdoptionReason(String adoptionReason) { this.adoptionReason = adoptionReason; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
}
