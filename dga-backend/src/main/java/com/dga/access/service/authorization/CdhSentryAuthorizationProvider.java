package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CdhSentryAuthorizationProvider implements AuthorizationProvider {

    @Value("${hive.server2.url}")
    private String fallbackHiveUrl;

    @Value("${hive.server2.username}")
    private String fallbackHiveUser;

    @Value("${hive.server2.password}")
    private String fallbackHivePassword;

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

    private JdbcTemplate jdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        String url = endpoint != null && endpoint.getUrl() != null ? endpoint.getUrl() : fallbackHiveUrl;
        String username = endpoint != null && endpoint.getUsername() != null ? endpoint.getUsername() : fallbackHiveUser;
        String password = endpoint != null && endpoint.getPassword() != null ? endpoint.getPassword() : fallbackHivePassword;

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
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
