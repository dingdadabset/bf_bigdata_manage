package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StarRocksSqlAuthorizationProvider implements AuthorizationProvider {

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_STARROCKS_JDBC)
                || AuthorizationSupport.hasAuthBackend(context, ClusterEndpoint.AUTH_STARROCKS_SQL)) {
            return true;
        }
        return cluster != null && cluster.getType() != null
                && cluster.getType().toUpperCase().contains("STARROCKS");
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
        AuthorizationSupport.validateName(username);
        try {
            return jdbcTemplate(context).queryForList("SHOW GRANTS FOR '" + username + "'");
        } catch (Exception e) {
            System.out.println("SHOW GRANTS FOR StarRocks user failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        validate(command);
        jdbcTemplate(context).execute(buildSql("GRANT", "TO", command));
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        validate(command);
        jdbcTemplate(context).execute(buildSql("REVOKE", "FROM", command));
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

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
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
