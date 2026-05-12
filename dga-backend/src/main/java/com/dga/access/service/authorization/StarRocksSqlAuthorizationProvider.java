package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

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
        List<Map<String, Object>> rows = jdbcTemplate(context).queryForList("SHOW USERS");
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
            List<Map<String, Object>> rows = jdbcTemplate(context).queryForList("SHOW GRANTS FOR " + formatUserIdentity(username));
            return normalizeGrantRows(rows);
        } catch (Exception e) {
            System.out.println("SHOW GRANTS FOR StarRocks user failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void createUser(AuthorizationContext context, String username, String password) {
        validateUserIdentity(username);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("StarRocks 用户密码不能为空");
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

    private void validate(GrantCommand command) {
        AuthorizationSupport.validateName(command.getUsername());
        AuthorizationSupport.validateName(command.getDatabase());
        if (command.getTable() != null && !command.getTable().isEmpty()) {
            AuthorizationSupport.validateName(command.getTable());
        }
        AuthorizationSupport.validatePermission(command.getPermission());
    }

    private String buildSql(String verb, String targetKeyword, GrantCommand command) {
        String privilege = toStarRocksPrivilege(command.getPermission());
        String resourceType;
        String resource;
        if (command.getTable() == null || command.getTable().isEmpty()) {
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
        return String.format("%s %s ON %s %s %s USER %s",
                verb, privilege, resourceType, resource, targetKeyword, formatUserIdentity(command.getUsername()));
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
