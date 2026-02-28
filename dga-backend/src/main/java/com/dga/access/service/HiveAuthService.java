package com.dga.access.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${hdp.hive.server2.url}")
    private String hdpHiveUrl;

    @Value("${hdp.hive.server2.username}")
    private String hdpHiveUser;

    @Value("${hdp.hive.server2.password}")
    private String hdpHivePassword;

    private JdbcTemplate hiveJdbcTemplate;
    private JdbcTemplate hdpJdbcTemplate;

    @Autowired
    private RangerService rangerService;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    @PostConstruct
    public void init() {
        // CDH
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(hiveUrl);
        dataSource.setUsername(hiveUser);
        dataSource.setPassword(hivePassword);
        this.hiveJdbcTemplate = new JdbcTemplate(dataSource);

        // HDP
        DriverManagerDataSource hdpDataSource = new DriverManagerDataSource();
        hdpDataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        hdpDataSource.setUrl(hdpHiveUrl);
        hdpDataSource.setUsername(hdpHiveUser);
        hdpDataSource.setPassword(hdpHivePassword);
        this.hdpJdbcTemplate = new JdbcTemplate(hdpDataSource);
    }

    private JdbcTemplate getTemplate(String clusterName) {
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            return hdpJdbcTemplate;
        }
        return hiveJdbcTemplate;
    }

    public List<String> listDatabases(String clusterName) {
        return getTemplate(clusterName).queryForList("SHOW DATABASES", String.class);
    }

    public List<String> listTables(String clusterName, String database) {
        validateName(database);
        String sql = "SHOW TABLES IN " + database;
        return getTemplate(clusterName).queryForList(sql, String.class);
    }

    public List<Map<String, Object>> getUserPermissions(String username, String clusterName) {
        validateName(username);

        // HDP Cluster uses Ranger
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            return rangerService.getUserPermissions(username);
        }

        List<Map<String, Object>> permissions = new java.util.ArrayList<>();
        JdbcTemplate template = getTemplate(clusterName);
        
        // 1. Direct User Grants
        try {
            String sql = "SHOW GRANT USER " + username;
            List<Map<String, Object>> userGrants = template.queryForList(sql);
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
            List<Map<String, Object>> roleGrants = template.queryForList(sql);
            permissions.addAll(roleGrants);
        } catch (Exception e) {
             // Role might not exist, ignore
        }
        
        return permissions;
    }

    public void grantPermission(String username, String database, String permission, String clusterName) {
        validateName(username);
        validateName(database);
        validatePermission(permission);

        // HDP Cluster uses Ranger
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            System.out.println("Using Ranger for HDP cluster grant: " + username + " on " + database);
            rangerService.grantPermission(username, database, null, permission);
            return;
        }
        
        JdbcTemplate template = getTemplate(clusterName);
        String sql = String.format("GRANT %s ON DATABASE %s TO USER %s", permission, database, username);
        System.out.println("Executing Hive SQL (" + clusterName + "): " + sql);
        try {
            template.execute(sql);
            System.out.println("Grant successful.");
        } catch (Exception e) {
            System.err.println("Failed to grant Hive permission: " + e.getMessage());
            
            // Handle Sentry restriction: Sentry does not allow granting to USER, must use ROLE
            if (e.getMessage().contains("Sentry does not allow privileges to be granted/revoked to/from: USER")) {
                System.out.println("Sentry detected. Attempting to grant via ROLE...");
                grantViaRole(username, database, null, permission, template);
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

    public void grantTablePermission(String username, String database, String table, String permission, String clusterName) {
        validateName(username);
        validateName(database);
        validateName(table);
        validatePermission(permission);

        // HDP Cluster uses Ranger
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            System.out.println("Using Ranger for HDP cluster table grant: " + username + " on " + database + "." + table);
            rangerService.grantPermission(username, database, table, permission);
            return;
        }
        
        JdbcTemplate template = getTemplate(clusterName);
        String sql = String.format("GRANT %s ON TABLE %s.%s TO USER %s", permission, database, table, username);
        System.out.println("Executing Hive SQL (" + clusterName + "): " + sql);
        try {
            template.execute(sql);
            System.out.println("Grant successful.");
        } catch (Exception e) {
            System.err.println("Failed to grant Hive table permission: " + e.getMessage());
            
            if (e.getMessage().contains("Sentry does not allow privileges to be granted/revoked to/from: USER")) {
                System.out.println("Sentry detected. Attempting to grant via ROLE...");
                grantViaRole(username, database, table, permission, template);
                return;
            }
            throw new RuntimeException("Hive Grant Error: " + e.getMessage());
        }
    }

    public void revokePermission(String username, String database, String permission, String clusterName) {
        validateName(username);
        validateName(database);
        validatePermission(permission);

        // HDP Cluster uses Ranger
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            System.out.println("Using Ranger for HDP cluster revoke: " + username + " on " + database);
            rangerService.revokePermission(username, database, null, permission);
            return;
        }
        
        JdbcTemplate template = getTemplate(clusterName);

        // Try Sentry ROLE syntax first as per user request
        try {
            revokeViaRole(username, database, null, permission, template);
            return;
        } catch (Exception e) {
             System.out.println("Sentry ROLE revoke failed or not applicable (" + e.getMessage() + "). Fallback to standard USER revoke...");
        }

        String sql = String.format("REVOKE %s ON DATABASE %s FROM USER %s", permission, database, username);
        System.out.println("Executing Hive SQL (" + clusterName + "): " + sql);
        try {
            template.execute(sql);
            System.out.println("Revoke successful.");
        } catch (Exception e) {
            System.err.println("Failed to revoke Hive permission: " + e.getMessage());
            throw new RuntimeException("Hive Revoke Error: " + e.getMessage());
        }
    }

    public void revokeTablePermission(String username, String database, String table, String permission, String clusterName) {
        validateName(username);
        validateName(database);
        validateName(table);
        validatePermission(permission);

        // HDP Cluster uses Ranger
        if (clusterName != null && clusterName.toUpperCase().contains("HDP")) {
            System.out.println("Using Ranger for HDP cluster table revoke: " + username + " on " + database + "." + table);
            rangerService.revokePermission(username, database, table, permission);
            return;
        }
        
        JdbcTemplate template = getTemplate(clusterName);

        // Try Sentry ROLE syntax first
        try {
            revokeViaRole(username, database, table, permission, template);
            return;
        } catch (Exception e) {
             System.out.println("Sentry ROLE revoke failed or not applicable (" + e.getMessage() + "). Fallback to standard USER revoke...");
        }

        String sql = String.format("REVOKE %s ON TABLE %s.%s FROM USER %s", permission, database, table, username);
        System.out.println("Executing Hive SQL (" + clusterName + "): " + sql);
        try {
            template.execute(sql);
            System.out.println("Revoke successful.");
        } catch (Exception e) {
            System.err.println("Failed to revoke Hive table permission: " + e.getMessage());
            throw new RuntimeException("Hive Revoke Error: " + e.getMessage());
        }
    }

    private void grantViaRole(String username, String database, String table, String permission, JdbcTemplate template) {
        String roleName = "role_" + username;

        if (!roleExists(roleName, template)) {
            try {
                template.execute("CREATE ROLE " + roleName);
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
            template.execute(grantSql);
            System.out.println("Granted to role: " + grantSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to grant to role " + roleName + ": " + e.getMessage());
        }

        String assignSql = String.format("GRANT ROLE %s TO GROUP %s", roleName, username);
        try {
            template.execute(assignSql);
            System.out.println("Assigned role to group: " + assignSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign role " + roleName + " to group " + username + ": " + e.getMessage());
        }
    }

    private void revokeViaRole(String username, String database, String table, String permission, JdbcTemplate template) {
        String roleName = "role_" + username;

        // Revoke from Role
        String revokeSql;
        if (table == null) {
            revokeSql = String.format("REVOKE %s ON DATABASE %s FROM ROLE %s", permission, database, roleName);
        } else {
            revokeSql = String.format("REVOKE %s ON TABLE %s.%s FROM ROLE %s", permission, database, table, roleName);
        }

        try {
            template.execute(revokeSql);
            System.out.println("Revoked from role: " + revokeSql);
        } catch (Exception e) {
            System.err.println("Revoke via role warning: " + e.getMessage());
            // Don't throw here, as we want to try to clean up the role if empty
        }
    }
    
    // Kept for backward compatibility but using default template? No, removing or updating is better.
    // Assuming internal usage is updated.
    
    public void revokeAll(String username, String clusterName) {
        validateName(username);
        JdbcTemplate template = getTemplate(clusterName);
        try {
            // Strategy 1: Drop the user-specific role if we are using the Role strategy
            String roleName = "role_" + username;
            if (roleExists(roleName, template)) {
                try {
                    template.execute("DROP ROLE " + roleName);
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
    }

    private boolean roleExists(String roleName, JdbcTemplate template) {
        try {
            List<String> roles = template.queryForList("SHOW ROLES", String.class);
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
