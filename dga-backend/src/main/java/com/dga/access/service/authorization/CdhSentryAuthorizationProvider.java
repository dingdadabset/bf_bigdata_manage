package com.dga.access.service.authorization;

import com.dga.access.service.LdapService;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.service.HiveServer2ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CdhSentryAuthorizationProvider implements AuthorizationProvider {

    @Value("${hive.server2.url}")
    private String fallbackHiveUrl;

    @Value("${hive.server2.username}")
    private String fallbackHiveUser;

    @Value("${hive.server2.password}")
    private String fallbackHivePassword;

    @Autowired
    private HiveServer2ConnectionService hiveServer2ConnectionService;

    @Autowired
    private LdapService ldapService;

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2)) {
            return true;
        }
        if (cluster == null || cluster.getType() == null) {
            return true;
        }
        String type = cluster.getType().toUpperCase();
        return type.contains("CDH") || type.contains("HIVE") || type.contains("EMR");
    }

    @Override
    public String engineType() {
        return "HIVE";
    }

    @Override
    public String authBackend() {
        return ClusterEndpoint.AUTH_SENTRY;
    }

    @Override
    public AuthorizationProviderDescriptor descriptor() {
        return AuthorizationProviderDescriptor.create(engineType(), authBackend(), ClusterEndpoint.TYPE_HIVE_SERVER2)
                .endpointRequired(false)
                .principalTypes("USER", "GROUP")
                .resourceTypes("DATABASE", "TABLE")
                .permissions("SELECT", "INSERT", "CREATE", "ALL")
                .requiresLdap(true)
                .identity("LDAP", "LDAP", false, true, "LDAP", true, true, "LDAP 用户")
                .rbac(true, true, true, false, true, false, false, false, "GROUP", "USER", "GROUP")
                .grant(true, true, false, true, true, true, false, true)
                .ui("LDAP 用户", "创建 LDAP 用户", "导入 LDAP 用户",
                        "CDH/Sentry 推荐授给 LDAP 组", "Sentry 角色对 LDAP 组原生生效，用户直绑会记录为待组映射");
    }

    @Override
    public List<String> listDatabases(AuthorizationContext context) {
        return jdbcTemplate(context).queryForList("SHOW DATABASES", String.class);
    }

    @Override
    public List<String> listTables(AuthorizationContext context, String database) {
        AuthorizationSupport.validateName(database);
        return jdbcTemplate(context).queryForList("SHOW TABLES IN " + database, String.class);
    }

    @Override
    public List<String> listPrincipals(AuthorizationContext context) {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        AuthorizationSupport.validateName(username);
        List<Map<String, Object>> permissions = new ArrayList<>();
        JdbcTemplate template = jdbcTemplate(context);

        try {
            permissions.addAll(template.queryForList("SHOW GRANT USER " + username));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("Sentry does not allow privileges")) {
                System.out.println("SHOW GRANT USER failed: " + msg);
            }
        }

        try {
            permissions.addAll(template.queryForList("SHOW GRANT ROLE role_" + username));
        } catch (Exception e) {
            // Role may not exist. This is expected for users without role-based grants.
        }

        for (String group : userLdapGroups(context, username)) {
            for (String role : rolesGrantedToGroup(template, group)) {
                try {
                    permissions.addAll(template.queryForList("SHOW GRANT ROLE " + role));
                } catch (Exception e) {
                    System.out.println("SHOW GRANT ROLE " + role + " failed: " + e.getMessage());
                }
            }
        }

        return permissions;
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        AuthorizationSupport.validateName(command.getUsername());
        AuthorizationSupport.validateName(command.getDatabase());
        AuthorizationSupport.validatePermission(command.getPermission());
        JdbcTemplate template = jdbcTemplate(context);

        String sql;
        if (command.getTable() == null || command.getTable().isEmpty()) {
            sql = String.format("GRANT %s ON DATABASE %s TO USER %s",
                    command.getPermission(), command.getDatabase(), command.getUsername());
        } else {
            AuthorizationSupport.validateName(command.getTable());
            sql = String.format("GRANT %s ON TABLE %s.%s TO USER %s",
                    command.getPermission(), command.getDatabase(), command.getTable(), command.getUsername());
        }

        try {
            template.execute(sql);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Sentry does not allow privileges to be granted/revoked to/from: USER")) {
                grantViaRole(command, template);
                return;
            }
            if (msg.contains("Grantee user") && msg.contains("doesn't exist")) {
                throw new RuntimeException("Hive Authorization Failed: User '" + command.getUsername()
                        + "' not found in Hive. Please wait for LDAP sync.");
            }
            throw new RuntimeException("Hive Grant Error: " + msg);
        }
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        AuthorizationSupport.validateName(command.getUsername());
        AuthorizationSupport.validateName(command.getDatabase());
        AuthorizationSupport.validatePermission(command.getPermission());
        JdbcTemplate template = jdbcTemplate(context);

        try {
            revokeViaRole(command, template);
            return;
        } catch (Exception e) {
            System.out.println("Sentry ROLE revoke failed or not applicable: " + e.getMessage());
        }

        String sql;
        if (command.getTable() == null || command.getTable().isEmpty()) {
            sql = String.format("REVOKE %s ON DATABASE %s FROM USER %s",
                    command.getPermission(), command.getDatabase(), command.getUsername());
        } else {
            AuthorizationSupport.validateName(command.getTable());
            sql = String.format("REVOKE %s ON TABLE %s.%s FROM USER %s",
                    command.getPermission(), command.getDatabase(), command.getTable(), command.getUsername());
        }

        try {
            template.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Hive Revoke Error: " + e.getMessage());
        }
    }

    @Override
    public void revokeAll(AuthorizationContext context, String username) {
        AuthorizationSupport.validateName(username);
        JdbcTemplate template = jdbcTemplate(context);
        String roleName = "role_" + username;
        if (roleExists(roleName, template)) {
            try {
                template.execute("DROP ROLE " + roleName);
            } catch (Exception e) {
                System.out.println("Role drop warning: " + e.getMessage());
            }
        }
    }

    @Override
    public void ensureRole(AuthorizationContext context, String roleCode) {
        AuthorizationSupport.validateName(roleCode);
        JdbcTemplate template = jdbcTemplate(context);
        if (roleExists(roleCode, template)) {
            return;
        }
        try {
            template.execute("CREATE ROLE " + roleCode);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (!msg.toLowerCase().contains("already")) {
                throw new RuntimeException("Failed to create role " + roleCode + ": " + msg);
            }
        }
    }

    @Override
    public void grantPermissionToRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        AuthorizationSupport.validateName(roleCode);
        validateDatabaseScope(database);
        AuthorizationSupport.validatePermission(permission);
        if (table == null || table.isEmpty()) {
            if (isAllDatabases(database)) {
                jdbcTemplate(context).execute(String.format("GRANT %s ON SERVER %s TO ROLE %s",
                        permission, sentryServerName(context), roleCode));
            } else {
                jdbcTemplate(context).execute(String.format("GRANT %s ON DATABASE %s TO ROLE %s", permission, database, roleCode));
            }
        } else {
            AuthorizationSupport.validateName(table);
            jdbcTemplate(context).execute(String.format("GRANT %s ON TABLE %s.%s TO ROLE %s", permission, database, table, roleCode));
        }
    }

    @Override
    public void revokePermissionFromRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        AuthorizationSupport.validateName(roleCode);
        validateDatabaseScope(database);
        AuthorizationSupport.validatePermission(permission);
        if (table == null || table.isEmpty()) {
            if (isAllDatabases(database)) {
                jdbcTemplate(context).execute(String.format("REVOKE %s ON SERVER %s FROM ROLE %s",
                        permission, sentryServerName(context), roleCode));
            } else {
                jdbcTemplate(context).execute(String.format("REVOKE %s ON DATABASE %s FROM ROLE %s", permission, database, roleCode));
            }
        } else {
            AuthorizationSupport.validateName(table);
            jdbcTemplate(context).execute(String.format("REVOKE %s ON TABLE %s.%s FROM ROLE %s", permission, database, table, roleCode));
        }
    }

    @Override
    public void assignRoleToGroup(AuthorizationContext context, String roleCode, String groupName) {
        AuthorizationSupport.validateName(roleCode);
        AuthorizationSupport.validateName(groupName);
        jdbcTemplate(context).execute(String.format("GRANT ROLE %s TO GROUP %s", roleCode, groupName));
    }

    @Override
    public void revokeRoleAssignment(AuthorizationContext context, String roleCode, String subjectType, String subjectName) {
        AuthorizationSupport.validateName(roleCode);
        AuthorizationSupport.validateName(subjectName);
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            jdbcTemplate(context).execute(String.format("REVOKE ROLE %s FROM GROUP %s", roleCode, subjectName));
        }
    }

    private JdbcTemplate jdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        if (endpoint != null && endpoint.getUrl() != null && !endpoint.getUrl().trim().isEmpty()) {
            return hiveServer2ConnectionService.jdbcTemplate(endpoint);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(fallbackHiveUrl);
        dataSource.setUsername(fallbackHiveUser);
        dataSource.setPassword(fallbackHivePassword);
        return new JdbcTemplate(dataSource);
    }

    private void validateDatabaseScope(String database) {
        if (!isAllDatabases(database)) {
            AuthorizationSupport.validateName(database);
        }
    }

    private boolean isAllDatabases(String database) {
        return "*".equals(database);
    }

    private String sentryServerName(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        String serverName = endpoint == null ? null : endpoint.getServiceName();
        if (serverName == null || serverName.trim().isEmpty()) {
            serverName = "server1";
        }
        AuthorizationSupport.validateName(serverName);
        return serverName;
    }

    @SuppressWarnings("unchecked")
    private Set<String> userLdapGroups(AuthorizationContext context, String username) {
        Set<String> groups = new LinkedHashSet<>();
        Cluster cluster = context.getCluster();
        String clusterIdentifier = cluster == null
                ? null
                : (cluster.getClusterCode() != null && !cluster.getClusterCode().trim().isEmpty()
                ? cluster.getClusterCode()
                : cluster.getClusterName());
        try {
            Map<String, Object> profile = ldapService.getUserLdapProfile(clusterIdentifier, username);
            Object primary = profile.get("primaryGroup");
            if (primary instanceof Map) {
                addGroupName(groups, ((Map<String, Object>) primary).get("name"));
            }
            Object supplementary = profile.get("supplementaryGroups");
            if (supplementary instanceof List) {
                for (Object item : (List<?>) supplementary) {
                    if (item instanceof Map) {
                        addGroupName(groups, ((Map<String, Object>) item).get("name"));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("LDAP group lookup failed for " + username + ": " + e.getMessage());
        }
        return groups;
    }

    private void addGroupName(Set<String> groups, Object value) {
        if (value == null) {
            return;
        }
        String group = String.valueOf(value).trim();
        if (!group.isEmpty()) {
            groups.add(group);
        }
    }

    private Set<String> rolesGrantedToGroup(JdbcTemplate template, String group) {
        Set<String> roles = new LinkedHashSet<>();
        try {
            AuthorizationSupport.validateName(group);
            List<Map<String, Object>> rows = template.queryForList("SHOW ROLE GRANT GROUP " + group);
            for (Map<String, Object> row : rows) {
                String role = roleNameFromRow(row);
                if (role != null && !role.trim().isEmpty()) {
                    roles.add(role.trim());
                }
            }
        } catch (Exception e) {
            System.out.println("SHOW ROLE GRANT GROUP " + group + " failed: " + e.getMessage());
        }
        return roles;
    }

    private String roleNameFromRow(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if ((key.contains("role") || key.equals("name")) && entry.getValue() != null) {
                return String.valueOf(entry.getValue());
            }
        }
        for (Object value : row.values()) {
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private void grantViaRole(GrantCommand command, JdbcTemplate template) {
        String roleName = "role_" + command.getUsername();
        if (!roleExists(roleName, template)) {
            try {
                template.execute("CREATE ROLE " + roleName);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (!msg.contains("already exists")) {
                    throw new RuntimeException("Failed to create role " + roleName + ": " + msg);
                }
            }
        }

        String grantSql;
        if (command.getTable() == null || command.getTable().isEmpty()) {
            grantSql = String.format("GRANT %s ON DATABASE %s TO ROLE %s",
                    command.getPermission(), command.getDatabase(), roleName);
        } else {
            grantSql = String.format("GRANT %s ON TABLE %s.%s TO ROLE %s",
                    command.getPermission(), command.getDatabase(), command.getTable(), roleName);
        }
        template.execute(grantSql);
        template.execute(String.format("GRANT ROLE %s TO GROUP %s", roleName, command.getUsername()));
    }

    private void revokeViaRole(GrantCommand command, JdbcTemplate template) {
        String roleName = "role_" + command.getUsername();
        String revokeSql;
        if (command.getTable() == null || command.getTable().isEmpty()) {
            revokeSql = String.format("REVOKE %s ON DATABASE %s FROM ROLE %s",
                    command.getPermission(), command.getDatabase(), roleName);
        } else {
            revokeSql = String.format("REVOKE %s ON TABLE %s.%s FROM ROLE %s",
                    command.getPermission(), command.getDatabase(), command.getTable(), roleName);
        }
        template.execute(revokeSql);
    }

    private boolean roleExists(String roleName, JdbcTemplate template) {
        try {
            List<String> roles = template.queryForList("SHOW ROLES", String.class);
            if (roles == null) {
                return false;
            }
            for (String role : roles) {
                if (role != null && role.trim().equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("SHOW ROLES failed or not supported: " + e.getMessage());
        }
        return false;
    }
}
