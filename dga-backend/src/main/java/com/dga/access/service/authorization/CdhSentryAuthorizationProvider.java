package com.dga.access.service.authorization;

import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRolePermissionSnapshot;
import com.dga.access.dto.BackendRoleSnapshot;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                .permissions("SELECT", "INSERT", "CREATE", "ALTER", "DROP", "ALL")
                .permissionLevel("READONLY", "只读", "LOW", "SELECT")
                .permissionLevel("WRITE", "读写", "MEDIUM", "SELECT", "INSERT")
                .permissionLevel("DDL", "结构变更", "HIGH", "CREATE", "ALTER", "DROP")
                .permissionLevel("ADMIN", "管理员", "HIGH", "ALL")
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
    public List<BackendRoleSnapshot> listBackendRoles(AuthorizationContext context, BackendRoleInventoryRequest request) {
        BackendRoleInventoryRequest inventoryRequest = request == null ? new BackendRoleInventoryRequest() : request;
        JdbcTemplate template = jdbcTemplate(context);
        Set<String> requestedRoles = normalizedRoleFilter(inventoryRequest.getRoleCodes());
        String keyword = trimToNull(inventoryRequest.getKeyword());
        List<String> roleNames = backendRoleNames(template);
        if (roleNames.isEmpty() && !requestedRoles.isEmpty()) {
            roleNames.addAll(requestedRoles);
        }
        Map<String, List<String>> groupNamesByRole = inventoryRequest.isIncludeAssignments()
                ? groupNamesByRole(context, template) : new HashMap<>();

        List<BackendRoleSnapshot> snapshots = new ArrayList<>();
        for (String roleName : roleNames) {
            String roleCode = trimToNull(roleName);
            if (roleCode == null || !matchesRoleFilter(roleCode, requestedRoles, keyword)) {
                continue;
            }
            BackendRoleSnapshot snapshot = new BackendRoleSnapshot();
            snapshot.setRoleCode(roleCode);
            snapshot.setRoleName(roleCode);
            snapshot.setCluster(context.getClusterCodeOrName());
            snapshot.setAuthBackend(authBackend());
            snapshot.setEngineType(engineType());
            if (inventoryRequest.isIncludePermissions()) {
                try {
                    snapshot.setPermissions(rolePermissions(template, roleCode));
                } catch (Exception e) {
                    snapshot.getWarnings().add("读取角色权限失败: " + compactErrorMessage(e));
                }
            }
            if (inventoryRequest.isIncludeAssignments()) {
                List<String> groups = groupNamesByRole.get(lower(roleCode));
                if (groups != null) {
                    snapshot.setGroupNames(groups);
                }
                snapshot.getWarnings().add("CDH/Sentry 无法稳定按角色直接反查 LDAP 组，已根据 LDAP 组清单尽力匹配；未列出的组需手动接管。");
            }
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public List<Map<String, Object>> getUserPermissions(AuthorizationContext context, String username) {
        AuthorizationSupport.validateName(username);
        List<Map<String, Object>> permissions = new ArrayList<>();
        JdbcTemplate template = jdbcTemplate(context);

        try {
            permissions.addAll(withGrantSource(template.queryForList("SHOW GRANT USER " + username), "USER", username, null));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("Sentry does not allow privileges")) {
                System.out.println("SHOW GRANT USER failed: " + msg);
            }
        }

        try {
            String userRole = "role_" + username;
            permissions.addAll(withGrantSource(template.queryForList("SHOW GRANT ROLE " + userRole), "USER_ROLE", userRole, username));
        } catch (Exception e) {
            // Role may not exist. This is expected for users without role-based grants.
        }

        for (String group : userLdapGroups(context, username)) {
            for (String role : rolesGrantedToGroup(template, group)) {
                try {
                    permissions.addAll(withGrantSource(template.queryForList("SHOW GRANT ROLE " + role), "GROUP_ROLE", role, group));
                } catch (Exception e) {
                    System.out.println("SHOW GRANT ROLE " + role + " failed: " + e.getMessage());
                }
            }
        }

        return permissions;
    }

    private List<String> backendRoleNames(JdbcTemplate template) {
        List<String> roles = new ArrayList<>();
        try {
            List<String> rows = template.queryForList("SHOW ROLES", String.class);
            if (rows != null) {
                for (String row : rows) {
                    String role = trimToNull(row);
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
        } catch (Exception e) {
            try {
                for (Map<String, Object> row : template.queryForList("SHOW ROLES")) {
                    String role = trimToNull(roleNameFromRow(row));
                    if (role != null) {
                        roles.add(role);
                    }
                }
            } catch (Exception nested) {
                throw new RuntimeException("SHOW ROLES failed: " + compactErrorMessage(nested));
            }
        }
        return roles;
    }

    private Map<String, List<String>> groupNamesByRole(AuthorizationContext context, JdbcTemplate template) {
        Map<String, LinkedHashSet<String>> groupsByRole = new HashMap<>();
        String clusterIdentifier = context.getClusterCodeOrName();
        try {
            List<Map<String, Object>> groups = ldapService.listPosixGroups(clusterIdentifier);
            if (groups == null) {
                return new HashMap<>();
            }
            for (Map<String, Object> group : groups) {
                String groupName = stringValue(group.get("name"));
                if (groupName == null) {
                    continue;
                }
                for (String role : rolesGrantedToGroup(template, groupName)) {
                    groupsByRole.computeIfAbsent(lower(role), key -> new LinkedHashSet<>()).add(groupName);
                }
            }
        } catch (Exception e) {
            System.out.println("LDAP group role inventory failed: " + e.getMessage());
        }
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : groupsByRole.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    private List<BackendRolePermissionSnapshot> rolePermissions(JdbcTemplate template, String roleCode) {
        AuthorizationSupport.validateName(roleCode);
        Map<String, BackendRolePermissionSnapshot> unique = new LinkedHashMap<>();
        for (Map<String, Object> row : template.queryForList("SHOW GRANT ROLE " + roleCode)) {
            BackendRolePermissionSnapshot permission = parseRolePermission(row);
            if (permission.getDatabaseName() == null || permission.getPermission() == null) {
                continue;
            }
            unique.putIfAbsent(permissionKey(permission), permission);
        }
        return new ArrayList<>(unique.values());
    }

    private BackendRolePermissionSnapshot parseRolePermission(Map<String, Object> row) {
        Map<String, Object> lowerRow = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (row != null) {
            lowerRow.putAll(row);
        }
        String grantText = pickString(lowerRow, "grants", "grant", "grant_stmt", "grant_statement", "privilege");
        if (grantText == null) {
            grantText = firstStringValue(row);
        }
        String database = pickString(lowerRow, "database", "database_name", "db", "db_name", "databaseName");
        String table = pickString(lowerRow, "table", "table_name", "tbl", "tableName");
        String permission = pickString(lowerRow, "privilege", "permission", "action");
        String resourceType = table == null || table.isEmpty() ? "DATABASE" : "TABLE";
        if (grantText != null && grantText.toUpperCase().startsWith("GRANT ")) {
            Map<String, String> parsed = parseGrantText(grantText);
            database = database != null ? database : parsed.get("database");
            table = table != null ? table : parsed.get("table");
            permission = permission != null ? permission : parsed.get("permission");
            resourceType = parsed.get("resourceType") != null ? parsed.get("resourceType") : resourceType;
        }
        table = normalizeAuthTable(table);
        if (table == null && !"GLOBAL".equals(resourceType)) {
            resourceType = "DATABASE";
        }
        BackendRolePermissionSnapshot snapshot = new BackendRolePermissionSnapshot();
        snapshot.setResourceType(resourceType);
        snapshot.setDatabaseName(database);
        snapshot.setTableName(table);
        snapshot.setPermission(normalizePermissionName(permission));
        snapshot.setRawGrant(grantText);
        return snapshot;
    }

    private Map<String, String> parseGrantText(String grantText) {
        Map<String, String> parsed = new HashMap<>();
        Matcher privilegeMatcher = Pattern.compile("(?i)^GRANT\\s+(.+?)\\s+ON\\s+").matcher(grantText);
        if (privilegeMatcher.find()) {
            parsed.put("permission", privilegeMatcher.group(1).trim());
        }
        if (Pattern.compile("(?i)ON\\s+SERVER\\s+").matcher(grantText).find()) {
            parsed.put("resourceType", "DATABASE");
            parsed.put("database", "*");
            return parsed;
        }
        Matcher tableMatcher = Pattern.compile("(?i)ON\\s+TABLE\\s+`?([^`\\.\\s]+)`?\\.`?([^`\\s]+)`?").matcher(grantText);
        if (tableMatcher.find()) {
            parsed.put("resourceType", "TABLE");
            parsed.put("database", tableMatcher.group(1));
            parsed.put("table", tableMatcher.group(2));
            return parsed;
        }
        Matcher databaseMatcher = Pattern.compile("(?i)ON\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (databaseMatcher.find()) {
            parsed.put("resourceType", "DATABASE");
            parsed.put("database", databaseMatcher.group(1));
        }
        return parsed;
    }

    private Set<String> normalizedRoleFilter(List<String> roleCodes) {
        Set<String> roles = new LinkedHashSet<>();
        if (roleCodes == null) {
            return roles;
        }
        for (String roleCode : roleCodes) {
            String role = trimToNull(roleCode);
            if (role != null) {
                roles.add(lower(role));
            }
        }
        return roles;
    }

    private boolean matchesRoleFilter(String roleCode, Set<String> requestedRoles, String keyword) {
        if (requestedRoles != null && !requestedRoles.isEmpty() && !requestedRoles.contains(lower(roleCode))) {
            return false;
        }
        return keyword == null || lower(roleCode).contains(lower(keyword));
    }

    private String permissionKey(BackendRolePermissionSnapshot permission) {
        return String.join("|",
                upper(permission.getResourceType()),
                lower(permission.getDatabaseName()),
                lower(permission.getTableName() == null ? "*" : permission.getTableName()),
                upper(permission.getPermission()));
    }

    private String normalizePermissionName(String permission) {
        String normalized = trimToNull(permission);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if (normalized.endsWith("_PRIV")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        if ("*".equals(normalized) || "ALL PRIVILEGES".equals(normalized) || "ALL_PRIVILEGES".equals(normalized)) {
            return "ALL";
        }
        return normalized;
    }

    private String normalizeAuthTable(String table) {
        String normalized = trimToNull(table);
        if (normalized == null || "*".equals(normalized) || "ALL TABLES".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String firstStringValue(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        for (Object value : row.values()) {
            String text = value == null ? null : trimToNull(String.valueOf(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String pickString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            String text = value == null ? null : trimToNull(String.valueOf(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String lower(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text.toLowerCase();
    }

    private String upper(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text.toUpperCase();
    }

    private List<Map<String, Object>> withGrantSource(List<Map<String, Object>> grants, String source, String sourceRole, String sourceGroup) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (grants == null) {
            return result;
        }
        for (Map<String, Object> grant : grants) {
            Map<String, Object> item = new HashMap<>();
            if (grant != null) {
                item.putAll(grant);
            }
            item.put("source", source);
            item.put("sourceRole", sourceRole);
            if (sourceGroup != null && !sourceGroup.trim().isEmpty()) {
                item.put("sourceGroup", sourceGroup);
            }
            result.add(item);
        }
        return result;
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
        if (command.getTable() != null && !command.getTable().isEmpty()) {
            AuthorizationSupport.validateName(command.getTable());
        }
        JdbcTemplate template = jdbcTemplate(context);
        String roleName = "role_" + command.getUsername();

        if (!roleExists(roleName, template)) {
            return;
        }

        try {
            revokeViaRole(command, template);
        } catch (Exception e) {
            if (!isBenignRevokeMiss(e)) {
                throw new RuntimeException("Hive Revoke Error: " + compactErrorMessage(e));
            }
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
            JdbcTemplate template = jdbcTemplate(context);
            if (!roleExists(roleCode, template)) {
                return;
            }
            try {
                template.execute(String.format("REVOKE ROLE %s FROM GROUP %s", roleCode, subjectName));
            } catch (Exception e) {
                if (!isBenignRevokeMiss(e)) {
                    throw new RuntimeException("Hive Role Assignment Revoke Error: " + compactErrorMessage(e));
                }
            }
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

    private boolean isBenignRevokeMiss(Exception e) {
        String message = compactErrorMessage(e).toLowerCase();
        return message.contains("doesn't exist")
                || message.contains("does not exist")
                || message.contains("not exist")
                || message.contains("not found")
                || message.contains("no privilege")
                || message.contains("doesn't have")
                || message.contains("does not have")
                || message.contains("not currently granted");
    }

    private String compactErrorMessage(Exception e) {
        String message = e == null || e.getMessage() == null ? "授权执行失败" : e.getMessage();
        String compact = message.replaceAll("\\s+", " ").trim();
        compact = compact.replaceAll("(?i)Server Stacktrace:.*$", "").trim();
        compact = compact.replaceAll("(?i);\\s*nested exception is.*$", "").trim();
        return compact.length() > 240 ? compact.substring(0, 240) + "..." : compact;
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
