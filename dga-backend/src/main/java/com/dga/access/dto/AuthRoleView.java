package com.dga.access.dto;

import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.entity.AuthUserRole;

import java.util.List;

public class AuthRoleView {
    private AuthRole role;
    private List<AuthRolePermission> permissions;
    private List<AuthUserRole> assignments;

    public AuthRoleView(AuthRole role, List<AuthRolePermission> permissions, List<AuthUserRole> assignments) {
        this.role = role;
        this.permissions = permissions;
        this.assignments = assignments;
    }

    public AuthRole getRole() { return role; }
    public void setRole(AuthRole role) { this.role = role; }
    public List<AuthRolePermission> getPermissions() { return permissions; }
    public void setPermissions(List<AuthRolePermission> permissions) { this.permissions = permissions; }
    public List<AuthUserRole> getAssignments() { return assignments; }
    public void setAssignments(List<AuthUserRole> assignments) { this.assignments = assignments; }
}
