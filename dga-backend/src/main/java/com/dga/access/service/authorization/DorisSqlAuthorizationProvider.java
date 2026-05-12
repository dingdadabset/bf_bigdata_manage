package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

@Service
public class DorisSqlAuthorizationProvider implements AuthorizationProvider {

    private static final String DEFAULT_CATALOG = "internal";

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_DORIS_JDBC)
                || AuthorizationSupport.hasAuthBackend(context, ClusterEndpoint.AUTH_DORIS_SQL)) {
            return true;
        }
        return cluster != null && cluster.getType() != null
                && cluster.getType().toUpperCase().contains("DORIS");
    }

    @Override
    public String engineType() {
        return "DORIS";
    }

    @Override
    public String authBackend() {
        return ClusterEndpoint.AUTH_DORIS_SQL;
    }

    @Override
    public List<String> listDatabases(AuthorizationContext context) {
        return jdbcTemplate(context).queryForList("SHOW DATABASES", String.class);
    }

    @Override
    public List<String> listTables(AuthorizationContext context, String database) {
        AuthorizationSupport.validateName(database);
        return jdbcTemplate(context).queryForList("SHOW TABLES FROM `" + database + "`", String.class);
    }

    @Override
    public List<String> listPrincipals(AuthorizationContext context) {
        List<Map<String, Object>> rows = jdbcTemplate(context).queryForList("SHOW ALL GRANTS");
        Set<String> users = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = caseInsensitive(row);
            String identity = stringValue(normalized.get("UserIdentity"));
            String user = normalizeUser(identity);
            if (user != null) {
                users.add(user);
            }
        }
        return new ArrayList<>(users);
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        AuthorizationSupport.validateName(username);
        try {
            List<Map<String, Object>> rows = jdbcTemplate(context)
                    .queryForList("SHOW GRANTS FOR " + formatUserIdentity(username));
            return normalizeGrantRows(rows);
        } catch (Exception e) {
            System.out.println("SHOW GRANTS FOR Doris user failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void createUser(AuthorizationContext context, String username, String password) {
        validateUserIdentity(username);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Doris 用户密码不能为空");
        }
        jdbcTemplate(context).execute("CREATE USER " + formatUserIdentity(username) + " IDENTIFIED BY '" + escapeSqlLiteral(password) + "'");
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        validate(command);
        jdbcTemplate(context).execute(buildSql("GRANT", "TO", command));
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        validate(command);
        String sql = buildSql("REVOKE", "FROM", command);
        try {
            jdbcTemplate(context).execute(sql);
        } catch (Exception e) {
            if (isMissingGrant(e)) {
                System.out.println("Doris grant already absent, skip revoke: " + sql);
                return;
            }
            throw e;
        }
    }

    @Override
    public void revokeAll(AuthorizationContext context, String username) {
        System.out.println("Doris revokeAll is not implemented because Doris grants are resource scoped.");
    }

    private JdbcTemplate jdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_DORIS_JDBC);
        if (endpoint == null) {
            throw new RuntimeException("Doris JDBC endpoint is required for cluster: " + context.getClusterCodeOrName());
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(endpoint.getUrl());
        dataSource.setUsername(endpoint.getUsername());
        dataSource.setPassword(endpoint.getPassword());
        return new JdbcTemplate(dataSource);
    }

    private void validate(GrantCommand command) {
        AuthorizationSupport.validateName(command.getUsername());
        AuthorizationSupport.validateName(command.getDatabase());
        if (command.getTable() != null && !command.getTable().isEmpty()) {
            AuthorizationSupport.validateName(command.getTable());
        }
        AuthorizationSupport.validatePermission(command.getPermission());
    }

    private String buildSql(String verb, String targetKeyword, GrantCommand command) {
        String privilege = toDorisPrivilege(command.getPermission());
        String privilegeLevel = command.getTable() == null || command.getTable().isEmpty()
                ? DEFAULT_CATALOG + "." + command.getDatabase() + ".*"
                : DEFAULT_CATALOG + "." + command.getDatabase() + "." + command.getTable();
        return String.format("%s %s ON %s %s %s",
                verb, privilege, privilegeLevel, targetKeyword, formatUserIdentity(command.getUsername()));
    }

    private String toDorisPrivilege(String permission) {
        String normalized = permission == null ? "" : permission.trim().toUpperCase();
        if ("ALL".equals(normalized)) {
            return "ALL";
        }
        if ("INSERT".equals(normalized)) {
            return "LOAD_PRIV";
        }
        if ("SELECT".equals(normalized)) {
            return "SELECT_PRIV";
        }
        if ("CREATE".equals(normalized)) {
            return "CREATE_PRIV";
        }
        if ("ALTER".equals(normalized)) {
            return "ALTER_PRIV";
        }
        if ("DROP".equals(normalized)) {
            return "DROP_PRIV";
        }
        return normalized;
    }

    private List<Map<String, Object>> normalizeGrantRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> grants = new ArrayList<>();
        if (rows == null) {
            return grants;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = caseInsensitive(row);
            expandPrivilegeCell(grants, "GLOBAL", "ALL DATABASES", null,
                    stringValue(normalized.get("GlobalPrivs")), row);
            expandPrivilegeCell(grants, "DATABASE", null, null,
                    stringValue(normalized.get("DatabasePrivs")), row);
            expandPrivilegeCell(grants, "TABLE", null, null,
                    stringValue(normalized.get("TablePrivs")), row);
            expandPrivilegeCell(grants, "RESOURCE", "RESOURCE", null,
                    stringValue(normalized.get("ResourcePrivs")), row);
            expandPrivilegeCell(grants, "WORKLOAD_GROUP", "WORKLOAD GROUP", null,
                    stringValue(normalized.get("WorkloadGroupPrivs")), row);
        }
        return grants;
    }

    private void expandPrivilegeCell(List<Map<String, Object>> grants, String defaultResourceType,
                                     String defaultDatabase, String defaultTable,
                                     String value, Map<String, Object> rawRow) {
        if (isBlankOrNull(value)) {
            return;
        }
        String[] entries = value.split(";");
        for (String entry : entries) {
            String text = entry.trim();
            if (isBlankOrNull(text)) {
                continue;
            }
            String scope = defaultDatabase;
            String privileges = text;
            int colonIndex = text.indexOf(':');
            if (colonIndex >= 0) {
                scope = text.substring(0, colonIndex).trim();
                privileges = text.substring(colonIndex + 1).trim();
            }
            String[] privilegeParts = privileges.split(",");
            for (String privilege : privilegeParts) {
                String permission = fromDorisPrivilege(privilege);
                if (permission == null) {
                    continue;
                }
                Map<String, Object> grant = new HashMap<>();
                Scope parsedScope = parseScope(scope, defaultResourceType);
                grant.put("resourceType", parsedScope.resourceType == null ? defaultResourceType : parsedScope.resourceType);
                grant.put("database", parsedScope.database == null ? defaultDatabase : parsedScope.database);
                grant.put("table", parsedScope.table == null ? defaultTable : parsedScope.table);
                grant.put("permission", permission);
                grant.put("grantText", text);
                grant.put("raw", rawRow);
                grants.add(grant);
            }
        }
    }

    private Scope parseScope(String scope, String defaultResourceType) {
        Scope parsed = new Scope();
        parsed.resourceType = defaultResourceType;
        if (scope == null || scope.trim().isEmpty()) {
            return parsed;
        }
        String[] parts = scope.trim().split("\\.");
        if (parts.length >= 2) {
            parsed.database = stripIdentifier(parts[1]);
        }
        if (parts.length >= 3 && !"*".equals(parts[2])) {
            parsed.table = stripIdentifier(parts[2]);
            parsed.resourceType = "TABLE";
        } else if (parts.length >= 2) {
            parsed.resourceType = "DATABASE";
        }
        if (parts.length == 1) {
            parsed.database = stripIdentifier(parts[0]);
        }
        return parsed;
    }

    private String fromDorisPrivilege(String privilege) {
        if (privilege == null) {
            return null;
        }
        String normalized = privilege.trim().toUpperCase();
        if (normalized.isEmpty() || "NULL".equals(normalized)) {
            return null;
        }
        normalized = normalized.replace("_PRIV", "");
        if ("LOAD".equals(normalized)) {
            return "INSERT";
        }
        if ("ADMIN".equals(normalized)) {
            return "ADMIN";
        }
        return normalized;
    }

    private Map<String, Object> caseInsensitive(Map<String, Object> row) {
        Map<String, Object> normalized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (row != null) {
            normalized.putAll(row);
        }
        return normalized;
    }

    private void validateUserIdentity(String username) {
        String user = username == null ? "" : username.trim();
        String host = "%";
        int atIndex = user.indexOf('@');
        if (atIndex > 0) {
            host = user.substring(atIndex + 1).trim();
            user = user.substring(0, atIndex).trim();
        }
        AuthorizationSupport.validateName(stripQuotes(user));
        if (!"%".equals(host)) {
            AuthorizationSupport.validateName(stripQuotes(host).replace(".", "_"));
        }
    }

    private String formatUserIdentity(String username) {
        String user = username == null ? "" : username.trim();
        String host = "%";
        int atIndex = user.indexOf('@');
        if (atIndex > 0) {
            host = user.substring(atIndex + 1).trim();
            user = user.substring(0, atIndex).trim();
        }
        user = stripQuotes(user);
        host = stripQuotes(host);
        return "'" + user.replace("'", "''") + "'@'" + host.replace("'", "''") + "'";
    }

    private String normalizeUser(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "NULL".equalsIgnoreCase(text)) {
            return null;
        }
        int atIndex = text.indexOf('@');
        if (atIndex > 0) {
            text = text.substring(0, atIndex);
        }
        return stripQuotes(text);
    }

    private String stripIdentifier(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("`", "").trim();
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.startsWith("'") && text.endsWith("'") && text.length() > 1) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private boolean isMissingGrant(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("no such grant")
                || lower.contains("not grant")
                || lower.contains("does not have privilege");
    }

    private boolean isBlankOrNull(String value) {
        return value == null || value.trim().isEmpty() || "NULL".equalsIgnoreCase(value.trim());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private static class Scope {
        private String resourceType;
        private String database;
        private String table;
    }
}
