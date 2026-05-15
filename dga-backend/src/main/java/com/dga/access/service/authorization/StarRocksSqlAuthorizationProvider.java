package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StarRocksSqlAuthorizationProvider implements AuthorizationProvider {

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_STARROCKS_JDBC)
                || AuthorizationSupport.hasAuthBackend(context, ClusterEndpoint.AUTH_STARROCKS_SQL)) {
            return true;
        }
        if (cluster == null || cluster.getType() == null) {
            return false;
        }
        String type = cluster.getType().toUpperCase().replaceAll("[^A-Z0-9]", "");
        return "SR".equals(type) || type.contains("STARROCKS") || type.contains("STAR");
    }

    @Override
    public String engineType() {
        return "STARROCKS";
    }

    @Override
    public String authBackend() {
        return ClusterEndpoint.AUTH_STARROCKS_SQL;
    }

    @Override
    public AuthorizationProviderDescriptor descriptor() {
        return AuthorizationProviderDescriptor.create(engineType(), authBackend(), ClusterEndpoint.TYPE_STARROCKS_JDBC)
                .missingEndpointWarning("StarRocks 授权需要 STARROCKS_JDBC 端点。")
                .principalTypes("USER", "GROUP")
                .resourceTypes("DATABASE", "TABLE")
                .permissions("SELECT", "INSERT", "CREATE", "ALTER", "DROP", "ALL")
                .requiresLdap(false)
                .identity("AUTH_BACKEND", "SQL", true, true, "AUTH_BACKEND", false, false, "StarRocks 用户")
                .rbac(true, true, true, true, true, false, false, false, "USER", "USER", "GROUP")
                .grant(true, true, true, true, true, true, false, true)
                .ui("StarRocks 用户", "创建 StarRocks 用户", "导入 StarRocks 用户",
                        "StarRocks 支持用户直绑，也支持外部组角色绑定", "StarRocks 原生角色对用户或外部组生效");
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
        List<Map<String, Object>> rows = queryPrivileged(context, "SHOW USERS");
        List<String> users = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                String user = normalizeUser(value);
                if (user != null && !users.contains(user)) {
                    users.add(user);
                }
                break;
            }
        }
        return users;
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        validateUserIdentity(username);
        try {
            List<Map<String, Object>> rows = queryPrivileged(context, "SHOW GRANTS FOR " + formatUserIdentity(username));
            return normalizeGrantRows(rows);
        } catch (Exception e) {
            System.out.println("SHOW GRANTS FOR StarRocks user failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean userExists(AuthorizationContext context, String username) {
        validateUserIdentity(username);
        try {
            queryPrivileged(context, "SHOW GRANTS FOR " + formatUserIdentity(username));
            return true;
        } catch (Exception e) {
            if (isMissingUser(e)) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void createUser(AuthorizationContext context, String username, String password) {
        validateUserIdentity(username);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("StarRocks 用户密码不能为空");
        }
        executePrivileged(context, "CREATE USER " + formatUserIdentity(username) + " IDENTIFIED BY '" + escapeSqlLiteral(password) + "'");
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        validate(command);
        executePrivileged(context, buildSql("GRANT", "TO", command));
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        validate(command);
        String sql = buildSql("REVOKE", "FROM", command);
        try {
            executePrivileged(context, sql);
        } catch (Exception e) {
            if (isMissingGrant(e)) {
                System.out.println("StarRocks grant already absent, skip revoke: " + sql);
                return;
            }
            throw e;
        }
    }

    @Override
    public void revokeAll(AuthorizationContext context, String username) {
        System.out.println("StarRocks revokeAll is not implemented because StarRocks grants are resource scoped.");
    }

    @Override
    public void ensureRole(AuthorizationContext context, String roleCode) {
        AuthorizationSupport.validateName(roleCode);
        try {
            executePrivileged(context, "CREATE ROLE " + quoteIdentifier(roleCode));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (!msg.contains("already") && !msg.contains("exists")) {
                throw e;
            }
        }
    }

    @Override
    public void grantPermissionToRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        AuthorizationSupport.validateName(roleCode);
        GrantCommand command = roleCommand(roleCode, context.getClusterIdentifier(), database, table, permission);
        executePrivileged(context, buildRoleSql("GRANT", "TO", command));
    }

    @Override
    public void revokePermissionFromRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        AuthorizationSupport.validateName(roleCode);
        GrantCommand command = roleCommand(roleCode, context.getClusterIdentifier(), database, table, permission);
        try {
            executePrivileged(context, buildRoleSql("REVOKE", "FROM", command));
        } catch (Exception e) {
            if (!isMissingGrant(e)) {
                throw e;
            }
        }
    }

    @Override
    public void assignRoleToUser(AuthorizationContext context, String roleCode, String username) {
        AuthorizationSupport.validateName(roleCode);
        validateUserIdentity(username);
        executePrivileged(context, "GRANT " + quoteIdentifier(roleCode) + " TO USER " + formatUserIdentity(username));
    }

    @Override
    public void assignRoleToGroup(AuthorizationContext context, String roleCode, String groupName) {
        AuthorizationSupport.validateName(roleCode);
        AuthorizationSupport.validateName(groupName);
        executePrivileged(context, "GRANT " + quoteIdentifier(roleCode) + " TO GROUP " + quoteIdentifier(groupName));
    }

    @Override
    public void revokeRoleAssignment(AuthorizationContext context, String roleCode, String subjectType, String subjectName) {
        AuthorizationSupport.validateName(roleCode);
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            AuthorizationSupport.validateName(subjectName);
            executePrivileged(context, "REVOKE " + quoteIdentifier(roleCode) + " FROM GROUP " + quoteIdentifier(subjectName));
            return;
        }
        validateUserIdentity(subjectName);
        executePrivileged(context, "REVOKE " + quoteIdentifier(roleCode) + " FROM USER " + formatUserIdentity(subjectName));
    }

    private JdbcTemplate jdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_STARROCKS_JDBC);
        if (endpoint == null) {
            throw new RuntimeException("StarRocks JDBC endpoint is required for cluster: " + context.getClusterCodeOrName());
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(endpoint.getUrl());
        dataSource.setUsername(endpoint.getUsername());
        dataSource.setPassword(endpoint.getPassword());
        return new JdbcTemplate(dataSource);
    }

    private void executePrivileged(AuthorizationContext context, String sql) {
        jdbcTemplate(context).execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                activateAvailableRoles(statement);
                statement.execute(sql);
            }
            return null;
        });
    }

    private List<Map<String, Object>> queryPrivileged(AuthorizationContext context, String sql) {
        return jdbcTemplate(context).execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
            try (Statement statement = connection.createStatement()) {
                activateAvailableRoles(statement);
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    ResultSetMetaData metadata = resultSet.getMetaData();
                    int columnCount = metadata.getColumnCount();
                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metadata.getColumnLabel(i);
                            if (columnName == null || columnName.isEmpty()) {
                                columnName = metadata.getColumnName(i);
                            }
                            row.put(columnName, resultSet.getObject(i));
                        }
                        rows.add(row);
                    }
                    return rows;
                }
            }
        });
    }

    private void activateAvailableRoles(Statement statement) {
        try {
            statement.execute("SET ROLE ALL");
            return;
        } catch (Exception first) {
            try {
                statement.execute("SET ROLE user_admin");
            } catch (Exception second) {
                System.out.println("StarRocks role activation skipped: " + second.getMessage());
            }
        }
    }

    private void validate(GrantCommand command) {
        AuthorizationSupport.validateName(command.getUsername());
        validateDatabaseScope(command.getDatabase());
        if (command.getTable() != null && !command.getTable().isEmpty()) {
            AuthorizationSupport.validateName(command.getTable());
        }
        AuthorizationSupport.validatePermission(command.getPermission());
    }

    private String buildSql(String verb, String targetKeyword, GrantCommand command) {
        return buildPrincipalSql(verb, targetKeyword, command, "USER " + formatUserIdentity(command.getUsername()));
    }

    private String buildRoleSql(String verb, String targetKeyword, GrantCommand command) {
        return buildPrincipalSql(verb, targetKeyword, command, "ROLE " + quoteIdentifier(command.getUsername()));
    }

    private String buildPrincipalSql(String verb, String targetKeyword, GrantCommand command, String principal) {
        String privilege = toStarRocksPrivilege(command.getPermission());
        String resourceType;
        String resource;
        if (command.getTable() == null || command.getTable().isEmpty()) {
            if (isAllDatabases(command.getDatabase())) {
                return String.format("%s %s ON ALL TABLES IN ALL DATABASES %s %s",
                        verb, privilege, targetKeyword, principal);
            }
            if ("CREATE TABLE".equals(privilege)) {
                resourceType = "DATABASE";
                resource = quoteIdentifier(command.getDatabase());
            } else {
                resourceType = "ALL TABLES IN DATABASE";
                resource = quoteIdentifier(command.getDatabase());
            }
        } else {
            resourceType = "TABLE";
            resource = quoteIdentifier(command.getDatabase()) + "." + quoteIdentifier(command.getTable());
        }
        return String.format("%s %s ON %s %s %s %s",
                verb, privilege, resourceType, resource, targetKeyword, principal);
    }

    private void validateDatabaseScope(String database) {
        if (!isAllDatabases(database)) {
            AuthorizationSupport.validateName(database);
        }
    }

    private boolean isAllDatabases(String database) {
        return "*".equals(database);
    }

    private GrantCommand roleCommand(String roleCode, String cluster, String database, String table, String permission) {
        GrantCommand command = new GrantCommand();
        command.setUsername(roleCode);
        command.setCluster(cluster);
        command.setDatabase(database);
        command.setTable(table);
        command.setPermission(permission);
        validate(command);
        return command;
    }

    private String toStarRocksPrivilege(String permission) {
        if ("ALL".equalsIgnoreCase(permission)) {
            return "ALL";
        }
        if ("CREATE".equalsIgnoreCase(permission)) {
            return "CREATE TABLE";
        }
        return permission.toUpperCase();
    }

    private List<Map<String, Object>> normalizeGrantRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> grants = new ArrayList<>();
        if (rows == null) {
            return grants;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = caseInsensitive(row);
            String grant = stringValue(normalized.get("Grants"));
            if (isBlankOrNull(grant)) {
                grant = firstGrantText(row);
            }
            parseGrantText(grants, grant, row);
        }
        return grants;
    }

    private void parseGrantText(List<Map<String, Object>> grants, String grantText, Map<String, Object> rawRow) {
        if (isBlankOrNull(grantText)) {
            return;
        }
        String upper = grantText.toUpperCase();
        int grantIndex = upper.indexOf("GRANT ");
        int onIndex = upper.indexOf(" ON ");
        if (grantIndex < 0 || onIndex < 0 || onIndex <= grantIndex) {
            return;
        }
        String privileges = grantText.substring(grantIndex + 6, onIndex).trim();
        String afterOn = grantText.substring(onIndex + 4).trim();
        String afterOnUpper = afterOn.toUpperCase();
        int toIndex = afterOnUpper.indexOf(" TO ");
        if (toIndex >= 0) {
            afterOn = afterOn.substring(0, toIndex).trim();
            afterOnUpper = afterOn.toUpperCase();
        }
        Scope scope = parseGrantScope(afterOn, afterOnUpper);
        for (String privilege : privileges.split(",")) {
            String permission = fromStarRocksPrivilege(privilege);
            if (permission == null) {
                continue;
            }
            Map<String, Object> grant = new HashMap<>();
            grant.put("resourceType", scope.resourceType);
            grant.put("database", scope.database);
            grant.put("table", scope.table);
            grant.put("permission", permission);
            grant.put("grantText", grantText);
            grant.put("raw", rawRow);
            grants.add(grant);
        }
    }

    private Scope parseGrantScope(String text, String upper) {
        Scope scope = new Scope();
        scope.resourceType = "GLOBAL";
        scope.database = "ALL DATABASES";
        if (upper.startsWith("ALL TABLES IN DATABASE ")) {
            scope.resourceType = "DATABASE";
            scope.database = stripIdentifier(text.substring("ALL TABLES IN DATABASE ".length()).trim());
        } else if (upper.startsWith("DATABASE ")) {
            scope.resourceType = "DATABASE";
            scope.database = stripIdentifier(text.substring("DATABASE ".length()).trim());
        } else if (upper.startsWith("TABLE ")) {
            scope.resourceType = "TABLE";
            String resource = text.substring("TABLE ".length()).trim();
            String[] parts = resource.split("\\.", 2);
            if (parts.length == 2) {
                scope.database = stripIdentifier(parts[0]);
                scope.table = stripIdentifier(parts[1]);
            } else {
                scope.table = stripIdentifier(resource);
            }
        } else if (upper.startsWith("SYSTEM")) {
            scope.resourceType = "SYSTEM";
            scope.database = "SYSTEM";
        }
        return scope;
    }

    private String fromStarRocksPrivilege(String privilege) {
        if (privilege == null) {
            return null;
        }
        String normalized = privilege.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("CREATE TABLE".equals(normalized)) {
            return "CREATE";
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

    private String firstGrantText(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        for (Object value : row.values()) {
            String text = stringValue(value);
            if (!isBlankOrNull(text) && text.toUpperCase().startsWith("GRANT ")) {
                return text;
            }
        }
        return null;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String stripIdentifier(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("`", "").trim();
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
        return message != null && message.toLowerCase().contains("no such grant defined");
    }

    private boolean isMissingUser(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("cannot find user")
                || lower.contains("unknown user")
                || lower.contains("no such user")
                || lower.contains("doesn't exist")
                || lower.contains("does not exist");
    }

    private String normalizeUser(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        int atIndex = text.indexOf('@');
        if (atIndex > 0) {
            text = text.substring(0, atIndex);
        }
        if (text.startsWith("'") && text.endsWith("'") && text.length() > 1) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }
}
