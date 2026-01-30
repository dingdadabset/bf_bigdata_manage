package com.dga.access.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class HiveAuthService {

    @Value("${hive.server2.url}")
    private String hiveUrl;

    @Value("${hive.server2.username}")
    private String hiveUser;

    @Value("${hive.server2.password}")
    private String hivePassword;

    private JdbcTemplate hiveJdbcTemplate;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    @PostConstruct
    public void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(hiveUrl);
        dataSource.setUsername(hiveUser);
        dataSource.setPassword(hivePassword);
        this.hiveJdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<String> listDatabases() {
        return hiveJdbcTemplate.queryForList("SHOW DATABASES", String.class);
    }

    public List<String> listTables(String database) {
        validateName(database);
        String sql = "SHOW TABLES IN " + database;
        return hiveJdbcTemplate.queryForList(sql, String.class);
    }

    public List<Map<String, Object>> getUserPermissions(String username) {
        validateName(username);
        List<Map<String, Object>> permissions = new java.util.ArrayList<>();
        
        // 1. Direct User Grants
        try {
            String sql = "SHOW GRANT USER " + username;
            List<Map<String, Object>> userGrants = hiveJdbcTemplate.queryForList(sql);
            permissions.addAll(userGrants);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Sentry does not allow privileges")) {
                // Sentry environment detected; USER grants are not supported. Ignore.
            } else {
                System.out.println("SHOW GRANT USER failed: " + msg);
            }
        }

        // 2. Role Grants (Sentry)
        String roleName = "role_" + username;
        try {
            String sql = "SHOW GRANT ROLE " + roleName;
            List<Map<String, Object>> roleGrants = hiveJdbcTemplate.queryForList(sql);
            permissions.addAll(roleGrants);
        } catch (Exception e) {
             // Role might not exist, ignore
        }
        
        return permissions;
    }

    public void grantPermission(String username, String database, String permission) {
        validateName(username);
        validateName(database);
        validatePermission(permission);
        
        String sql = String.format("GRANT %s ON DATABASE %s TO USER %s", permission, database, username);
        System.out.println("Executing Hive SQL: " + sql);
        try {
            hiveJdbcTemplate.execute(sql);
            System.out.println("Grant successful.");
        } catch (Exception e) {
            System.err.println("Failed to grant Hive permission: " + e.getMessage());
            
            // Handle Sentry restriction: Sentry does not allow granting to USER, must use ROLE
            if (e.getMessage().contains("Sentry does not allow privileges to be granted/revoked to/from: USER")) {
                System.out.println("Sentry detected. Attempting to grant via ROLE...");
                grantViaRole(username, database, null, permission);
                return;
            }

            // If error 40000 (User doesn't exist), it means Hive/Ranger doesn't see the new LDAP user yet.
            if (e.getMessage().contains("Grantee user") && e.getMessage().contains("doesn't exist")) {
                 System.err.println("WARNING: User created in LDAP but not yet visible to Hive/Ranger. Sync latency?");
                 throw new RuntimeException("Hive Authorization Failed: User '" + username + "' not found in Hive. Please wait for LDAP Sync (approx 60s).");
            }
            throw new RuntimeException("Hive Grant Error: " + e.getMessage());
        }
    }

    public void grantTablePermission(String username, String database, String table, String permission) {
        validateName(username);
        validateName(database);
        validateName(table);
        validatePermission(permission);
        String sql = String.format("GRANT %s ON TABLE %s.%s TO USER %s", permission, database, table, username);
        System.out.println("Executing Hive SQL: " + sql);
        try {
            hiveJdbcTemplate.execute(sql);
            System.out.println("Grant successful.");
        } catch (Exception e) {
            System.err.println("Failed to grant Hive table permission: " + e.getMessage());
            
            if (e.getMessage().contains("Sentry does not allow privileges to be granted/revoked to/from: USER")) {
                System.out.println("Sentry detected. Attempting to grant via ROLE...");
                grantViaRole(username, database, table, permission);
                return;
            }
            throw new RuntimeException("Hive Grant Error: " + e.getMessage());
        }
    }

    public void revokePermission(String username, String database, String permission) {
        validateName(username);
        validateName(database);
        validatePermission(permission);

        // Try Sentry ROLE syntax first as per user request
        try {
            revokeViaRole(username, database, null, permission);
            return;
        } catch (Exception e) {
             System.out.println("Sentry ROLE revoke failed or not applicable (" + e.getMessage() + "). Fallback to standard USER revoke...");
        }

        String sql = String.format("REVOKE %s ON DATABASE %s FROM USER %s", permission, database, username);
        System.out.println("Executing Hive SQL: " + sql);
        try {
            hiveJdbcTemplate.execute(sql);
            System.out.println("Revoke successful.");
        } catch (Exception e) {
            System.err.println("Failed to revoke Hive permission: " + e.getMessage());
            throw new RuntimeException("Hive Revoke Error: " + e.getMessage());
        }
    }

    public void revokeTablePermission(String username, String database, String table, String permission) {
        validateName(username);
        validateName(database);
        validateName(table);
        validatePermission(permission);

        // Try Sentry ROLE syntax first
        try {
            revokeViaRole(username, database, table, permission);
            return;
        } catch (Exception e) {
             System.out.println("Sentry ROLE revoke failed or not applicable (" + e.getMessage() + "). Fallback to standard USER revoke...");
        }

        String sql = String.format("REVOKE %s ON TABLE %s.%s FROM USER %s", permission, database, table, username);
        System.out.println("Executing Hive SQL: " + sql);
        try {
            hiveJdbcTemplate.execute(sql);
            System.out.println("Revoke successful.");
        } catch (Exception e) {
            System.err.println("Failed to revoke Hive table permission: " + e.getMessage());
            throw new RuntimeException("Hive Revoke Error: " + e.getMessage());
        }
    }

    private void grantViaRole(String username, String database, String table, String permission) {
        String roleName = "role_" + username;

        if (!roleExists(roleName)) {
            try {
                hiveJdbcTemplate.execute("CREATE ROLE " + roleName);
                System.out.println("Created role: " + roleName);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("already exists")) {
                    System.out.println("Role already exists: " + roleName);
                } else {
                    throw new RuntimeException("Failed to create role " + roleName + ": " + msg);
                }
            }
        } else {
            System.out.println("Role exists, skip creation: " + roleName);
        }

        // 2. Grant to Role
        String grantSql;
        if (table == null) {
            grantSql = String.format("GRANT %s ON DATABASE %s TO ROLE %s", permission, database, roleName);
        } else {
            grantSql = String.format("GRANT %s ON TABLE %s.%s TO ROLE %s", permission, database, table, roleName);
        }
        
        try {
            hiveJdbcTemplate.execute(grantSql);
            System.out.println("Granted to role: " + grantSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to grant to role " + roleName + ": " + e.getMessage());
        }

        String assignSql = String.format("GRANT ROLE %s TO GROUP %s", roleName, username);
        try {
            hiveJdbcTemplate.execute(assignSql);
            System.out.println("Assigned role to group: " + assignSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign role " + roleName + " to group " + username + ": " + e.getMessage());
        }
    }

    private void revokeViaRole(String username, String database, String table, String permission) {
        String roleName = "role_" + username;

        // Revoke from Role
        String revokeSql;
        if (table == null) {
            revokeSql = String.format("REVOKE %s ON DATABASE %s FROM ROLE %s", permission, database, roleName);
        } else {
            revokeSql = String.format("REVOKE %s ON TABLE %s.%s FROM ROLE %s", permission, database, table, roleName);
        }

        try {
            hiveJdbcTemplate.execute(revokeSql);
            System.out.println("Revoked from role: " + revokeSql);
        } catch (Exception e) {
            System.err.println("Revoke via role warning: " + e.getMessage());
            // Don't throw here, as we want to try to clean up the role if empty
        }
    }

    public void revokeAll(String username) {
        validateName(username);
        try {
            // Strategy 1: Drop the user-specific role if we are using the Role strategy
            String roleName = "role_" + username;
            if (roleExists(roleName)) {
                try {
                    hiveJdbcTemplate.execute("DROP ROLE " + roleName);
                    System.out.println("Dropped role: " + roleName);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    System.out.println("Role drop warning: " + msg);
                }
            } else {
                System.out.println("Role not found, skip drop: " + roleName);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            System.out.println("Revoke-all warning: " + msg);
        }

        // Strategy 2: Revoke all direct privileges (if Sentry allows or not using Sentry)
        // Note: Hive doesn't support "REVOKE ALL FROM USER X" easily without knowing object.
        // But if we are strict about "Sentry via Role", dropping role handles it.
        // If native Hive:
        // We can't easily iterate all permissions here without Metastore access.
        // Assuming Role strategy is primary for this system. 
        // If not, we rely on the Access Log table to find what to revoke? 
        // Or we assume the user has no permissions left if we drop the role.
    }

    private boolean roleExists(String roleName) {
        try {
            List<String> roles = hiveJdbcTemplate.queryForList("SHOW ROLES", String.class);
            if (roles == null) return false;
            for (String r : roles) {
                if (r != null && r.trim().equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("SHOW ROLES failed or not supported: " + e.getMessage());
            return false;
        }
    }

    private void validateName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name");
        }
    }

    private void validatePermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Invalid permission");
        }
        String upper = permission.toUpperCase();
        if (!"ALL".equals(upper) && !"SELECT".equals(upper) && !"INSERT".equals(upper) && !"CREATE".equals(upper)) {
            throw new IllegalArgumentException("Invalid permission");
        }
    }
}
