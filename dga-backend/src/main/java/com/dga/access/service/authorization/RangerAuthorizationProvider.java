package com.dga.access.service.authorization;

import com.dga.access.service.LdapService;
import com.dga.access.service.RangerService;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.service.HiveServer2ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RangerAuthorizationProvider implements AuthorizationProvider {

    @Autowired
    private RangerService rangerService;

    @Autowired
    private LdapService ldapService;

    @Value("${hdp.hive.server2.url}")
    private String hdpHiveUrl;

    @Value("${hdp.hive.server2.username}")
    private String hdpHiveUser;

    @Value("${hdp.hive.server2.password}")
    private String hdpHivePassword;

    @Autowired
    private HiveServer2ConnectionService hiveServer2ConnectionService;

    @Override
    public boolean supports(AuthorizationContext context) {
        Cluster cluster = context.getCluster();
        if (AuthorizationSupport.hasAuthBackend(context, ClusterEndpoint.AUTH_RANGER)
                || AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_RANGER)) {
            return true;
        }
        String identifier = context.getClusterIdentifier();
        if (identifier != null && identifier.toUpperCase().contains("HDP")) {
            return true;
        }
        return cluster != null && cluster.getType() != null && cluster.getType().toUpperCase().contains("HDP");
    }

    @Override
    public String engineType() {
        return "HIVE";
    }

    @Override
    public String authBackend() {
        return ClusterEndpoint.AUTH_RANGER;
    }

    @Override
    public AuthorizationProviderDescriptor descriptor() {
        return AuthorizationProviderDescriptor.create(engineType(), authBackend(), ClusterEndpoint.TYPE_RANGER)
                .missingEndpointWarning("Ranger 授权需要 RANGER 端点。")
                .principalTypes("USER", "GROUP")
                .resourceTypes("DATABASE", "TABLE")
                .permissions("SELECT", "INSERT", "CREATE", "ALL")
                .requiresLdap(true)
                .identity("LDAP", "LDAP", false, true, "LDAP", true, true, "LDAP 用户")
                .rbac(true, true, true, false, false, true, true, true, "USER", "USER", "GROUP")
                .grant(true, true, true, true, true, true, false, true)
                .ui("LDAP 用户", "创建 LDAP 用户", "导入 LDAP 用户",
                        "Ranger 支持用户或 LDAP 组策略授权", "Ranger 无原生数据库角色，DGA 会将角色权限物化为策略");
    }

    @Override
    public List<String> listDatabases(AuthorizationContext context) {
        try {
            return hdpJdbcTemplate(context).queryForList("SHOW DATABASES", String.class);
        } catch (Exception e) {
            System.err.println("HDP HiveServer2 list databases failed, fallback to Ranger policies: " + e.getMessage());
            return rangerService.listPolicyDatabases(rangerEndpoint(context));
        }
    }

    @Override
    public List<String> listTables(AuthorizationContext context, String database) {
        AuthorizationSupport.validateName(database);
        try {
            return hdpJdbcTemplate(context).queryForList("SHOW TABLES IN " + database, String.class);
        } catch (Exception e) {
            System.err.println("HDP HiveServer2 list tables failed, fallback to Ranger policies: " + e.getMessage());
            return rangerService.listPolicyTables(rangerEndpoint(context), database);
        }
    }

    @Override
    public List<String> listPrincipals(AuthorizationContext context) {
        return rangerService.listUsers(rangerEndpoint(context));
    }

    @Override
    public List<String> listGroups(AuthorizationContext context) {
        return rangerService.listGroups(rangerEndpoint(context));
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        return rangerService.getUserPermissions(username, userLdapGroups(context, username), rangerEndpoint(context));
    }

    @Override
    public void grant(AuthorizationContext context, GrantCommand command) {
        rangerService.grantPermission(command.getUsername(), command.getDatabase(), command.getTable(),
                command.getPermission(), rangerEndpoint(context));
    }

    @Override
    public void ensureRole(AuthorizationContext context, String roleCode) {
        // Ranger does not expose a native database role primitive. DGA persists the role locally
        // and materializes concrete policy items when the role is assigned.
    }

    @Override
    public void grantPermissionToRole(AuthorizationContext context, String roleCode, String database, String table, String permission) {
        // Ranger has no database role primitive. DGA stores the role, and concrete policy items
        // are written when the role is assigned to users or groups.
    }

    @Override
    public void assignRoleToUser(AuthorizationContext context, String roleCode, String username) {
        // No native backend operation. Role assignment is tracked in DGA and expanded to policies.
    }

    @Override
    public void assignRoleToGroup(AuthorizationContext context, String roleCode, String groupName) {
        // No native backend operation. Role assignment is tracked in DGA and expanded to policies.
    }

    @Override
    public void grantPermissionToGroup(AuthorizationContext context, String groupName, String database, String table, String permission) {
        rangerService.grantGroupPermission(groupName, database, table, permission, rangerEndpoint(context));
    }

    @Override
    public void revokePermissionFromGroup(AuthorizationContext context, String groupName, String database, String table, String permission) {
        rangerService.revokeGroupPermission(groupName, database, table, permission, rangerEndpoint(context));
    }

    @Override
    public void revoke(AuthorizationContext context, RevokeCommand command) {
        rangerService.revokePermission(command.getUsername(), command.getDatabase(), command.getTable(),
                command.getPermission(), rangerEndpoint(context));
    }

    @Override
    public void revokeAll(AuthorizationContext context, String username) {
        System.out.println("Ranger revokeAll is not implemented because Ranger policies are resource scoped.");
    }

    private org.springframework.jdbc.core.JdbcTemplate hdpJdbcTemplate(AuthorizationContext context) {
        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        if (endpoint != null && endpoint.getUrl() != null && !endpoint.getUrl().trim().isEmpty()) {
            return hiveServer2ConnectionService.jdbcTemplate(endpoint);
        }
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
        dataSource.setUrl(hdpHiveUrl);
        dataSource.setUsername(hdpHiveUser);
        dataSource.setPassword(hdpHivePassword);
        return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
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
            System.out.println("LDAP group lookup failed for Ranger user " + username + ": " + e.getMessage());
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

    private ClusterEndpoint rangerEndpoint(AuthorizationContext context) {
        return AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_RANGER);
    }
}
