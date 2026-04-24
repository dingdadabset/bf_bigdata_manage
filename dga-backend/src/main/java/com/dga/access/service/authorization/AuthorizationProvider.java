package com.dga.access.service.authorization;

import java.util.List;
import java.util.Map;

public interface AuthorizationProvider {
    boolean supports(AuthorizationContext context);
    String engineType();
    String authBackend();
    List<String> listDatabases(AuthorizationContext context);
    List<String> listTables(AuthorizationContext context, String database);
    List<String> listPrincipals(AuthorizationContext context);
    List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username);
    void grant(AuthorizationContext context, GrantCommand command);
    void revoke(AuthorizationContext context, RevokeCommand command);
    void revokeAll(AuthorizationContext context, String username);
}
