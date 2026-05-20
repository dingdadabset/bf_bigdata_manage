package com.dga.access.service.authorization;

import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRoleSnapshot;

import java.util.List;
import java.util.Map;

public interface AuthorizationProvider {
    boolean supports(AuthorizationContext context);
    String engineType();
    String authBackend();
    AuthorizationProviderDescriptor descriptor();
    List<String> listDatabases(AuthorizationContext context);
    List<String> listTables(AuthorizationContext context, String database);
    List<String> listPrincipals(AuthorizationContext context);
    default List<String> listGroups(AuthorizationContext context) {
        return java.util.Collections.emptyList();
    }
    default List<BackendRoleSnapshot> listBackendRoles(AuthorizationContext context, BackendRoleInventoryRequest request) {
        throw new UnsupportedOperationException("Backend role inventory is not supported by " + engineType());
    }
    List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username);
    default boolean userExists(AuthorizationContext context, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        List<String> principals = listPrincipals(context);
        if (principals == null) {
            return false;
        }
        for (String principal : principals) {
            if (principal != null && username.trim().equalsIgnoreCase(principal.trim())) {
                return true;
            }
        }
        return false;
    }
    default void createUser(AuthorizationContext context, String username, String password) {
        throw new UnsupportedOperationException("Create user is not supported by " + engineType());
    }
    void grant(AuthorizationContext context, GrantCommand command);
    void revoke(AuthorizationContext context, RevokeCommand command);
    void revokeAll(AuthorizationContext context, String username);
    default void ensureRole(AuthorizationContext context, String roleCode) {
        throw new UnsupportedOperationException("Role is not supported by " + engineType());
    }
    default void grantPermissionToRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        throw new UnsupportedOperationException("Role permission is not supported by " + engineType());
    }
    default void grantPermissionToGroup(AuthorizationContext context, String groupName, String database, String table, String permission) {
        throw new UnsupportedOperationException("Group permission is not supported by " + engineType());
    }
    default void revokePermissionFromGroup(AuthorizationContext context, String groupName, String database, String table, String permission) {
        throw new UnsupportedOperationException("Group permission revoke is not supported by " + engineType());
    }
    default void revokePermissionFromRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        throw new UnsupportedOperationException("Role permission revoke is not supported by " + engineType());
    }
    default void assignRoleToUser(AuthorizationContext context, String roleCode, String username) {
        throw new UnsupportedOperationException("User role assignment is not supported by " + engineType());
    }
    default void assignRoleToGroup(AuthorizationContext context, String roleCode, String groupName) {
        throw new UnsupportedOperationException("Group role assignment is not supported by " + engineType());
    }
    default void revokeRoleAssignment(AuthorizationContext context, String roleCode, String subjectType, String subjectName) {
        throw new UnsupportedOperationException("Role assignment revoke is not supported by " + engineType());
    }
}
