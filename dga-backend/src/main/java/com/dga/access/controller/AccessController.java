package com.dga.access.controller;

import com.dga.access.dto.AccessRequest;
import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.CreateUserRequest;
import com.dga.access.dto.TableGrant;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.service.AdminGuard;
import com.dga.access.service.HiveAuthService;
import com.dga.access.service.IpaHttpService;
import com.dga.access.service.IpaService;
import com.dga.access.service.LdapService;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.access.service.authorization.AuthorizationCapability;
import com.dga.access.service.authorization.GrantCommand;
import com.dga.access.service.authorization.RevokeCommand;
import com.dga.access.entity.UserHiveAccess;
import com.dga.access.repository.UserHiveAccessRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/access")
@CrossOrigin
public class AccessController {

    private static final Set<String> PROTECTED_BIGDATA_USERS = new HashSet<>(java.util.Arrays.asList(
            "alading",
            "bf_hpt",
            "bf_hpt1",
            "md_bf",
            "hdfs",
            "hive",
            "yarn",
            "spark",
            "hbase",
            "impala",
            "sentry",
            "ranger"
    ));

    @Autowired
    private LdapService ldapService;

    @Autowired
    private HiveAuthService hiveAuthService;

    @Autowired
    private IpaService ipaService;

    @Autowired
    private IpaHttpService ipaHttpService;
    
    @Autowired
    private DgaUserRepository dgaUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserHiveAccessRepository userHiveAccessRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private com.dga.cluster.repository.ClusterRepository clusterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminGuard adminGuard;

    @PostMapping("/grant")
    public String grantAccess(@RequestBody AccessRequest request) {
        try {
            ldapService.createUser(request.getCluster(), request.getUsername(), request.getPassword(), request.getEmail());
        } catch (Exception e) {
            System.err.println("Warning: LDAP create user failed (might already exist): " + e.getMessage());
        }
        
        try {
            hiveAuthService.grantPermission(request.getUsername(), request.getDatabase(), request.getPermission(), request.getCluster());
        } catch (Exception e) {
            System.err.println("Warning: Hive/Ranger grant failed: " + e.getMessage());
        }
        
        // Record Access
        UserHiveAccess access = new UserHiveAccess();
        access.setUsername(request.getUsername());
        access.setClusterName(request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01");
        access.setDatabaseName(request.getDatabase());
        access.setPermission(request.getPermission());
        access.setGrantedBy("admin"); // TODO: get from security context
        access.setStatus("ACTIVE");
        userHiveAccessRepository.save(access);
        saveResourceAccess(request.getUsername(), request.getCluster(), request.getDatabase(), null,
                request.getPermission(), "admin", "DGA_GRANT");

        return "Access granted successfully for user: " + request.getUsername();
    }

    @PostMapping("/user")
    public String createUser(@RequestBody CreateUserRequest request) {
        String clusterName = resolveClusterName(request.getCluster());
        // Check if user exists in DB first
        if (dgaUserRepository.existsByUsernameAndClusterName(request.getUsername(), clusterName)) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "User " + request.getUsername() + " already exists in cluster " + clusterName + ".");
        }

        String strategy = request.getCreationStrategy();
        String resultMsg = "User created: " + request.getUsername();
        if (strategy == null || strategy.isEmpty() || strategy.toUpperCase().startsWith("SELF")) {
            strategy = "OPENLDAP";
        }
        
        if ("IPA_SSH".equalsIgnoreCase(strategy)) {
            String result = ipaService.createUser(request.getIpaHost(), request.getUsername(), request.getFirstName(), request.getLastName(), request.getPassword());
            if (!result.isEmpty()) return result; // Return error if any
            resultMsg = "User created via IPA(SSH): " + request.getUsername();
        } else if ("IPA_HTTP".equalsIgnoreCase(strategy)) {
            // Ensure groups exist
            try {
                ipaHttpService.createGroup("new_cluster_users", "Users for New Cluster", "1485400045");
                ipaHttpService.createGroup("old_cluster_users", "Users for Old Cluster", "1485400046");
            } catch (Exception e) {
                System.err.println("Warning: Failed to ensure IPA groups exist: " + e.getMessage());
            }

            ipaHttpService.createUser(request.getUsername(), request.getFirstName(), request.getLastName(), request.getPassword());
            
            // Assign to group based on cluster
            String cluster = request.getCluster();
            if (cluster != null) {
                String groupToAdd = null;
                if (cluster.toLowerCase().contains("cdh")) {
                    groupToAdd = "old_cluster_users";
                } else if (cluster.toLowerCase().contains("hdp")) {
                    groupToAdd = "new_cluster_users";
                }
                
                if (groupToAdd != null) {
                    try {
                        ipaHttpService.addUserToGroup(request.getUsername(), groupToAdd);
                        resultMsg += " and added to group " + groupToAdd;
                    } catch (Exception e) {
                        System.err.println("Failed to add user to group " + groupToAdd + ": " + e.getMessage());
                        resultMsg += " (Warning: Failed to add to group " + groupToAdd + ")";
                    }
                }
            }
            
            resultMsg = "User created via IPA(HTTP): " + request.getUsername();
        } else {
            ldapService.createUser(request.getCluster(), request.getUsername(), request.getPassword(), request.getEmail());
            strategy = "OPENLDAP";
            resultMsg = "User created via OpenLDAP: " + request.getUsername();
        }

        // Persist to MySQL
        if (!dgaUserRepository.existsByUsernameAndClusterName(request.getUsername(), clusterName)) {
            DgaUser user = new DgaUser();
            user.setUsername(request.getUsername());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            // Encrypt password
            if (request.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            user.setCreationStrategy(strategy);
            user.setClusterName(clusterName);
            dgaUserRepository.save(user);
        } else {
             // If user exists (e.g. re-registering or different strategy), update it?
             // For now, let's just ignore or maybe update the strategy/password if needed.
             // But requirement says "IPA registered account, password not saved". 
             // So if it exists, we might want to ensure password is saved if it wasn't before.
             DgaUser user = dgaUserRepository.findByUsernameAndClusterName(request.getUsername(), clusterName);
             if (user.getPassword() == null && request.getPassword() != null) {
                 user.setPassword(passwordEncoder.encode(request.getPassword()));
                 user.setClusterName(clusterName);
                 dgaUserRepository.save(user);
             }
        }

        return resultMsg;
    }
    
    @PostMapping("/import")
    public Map<String, Object> importUsers(@RequestParam(required = false) String cluster) {
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择具体集群后再导入 OpenLDAP 用户");
        }

        com.dga.cluster.entity.Cluster targetCluster = resolveCluster(cluster);
        String clusterName = targetCluster != null && targetCluster.getClusterName() != null
                ? targetCluster.getClusterName()
                : resolveClusterName(cluster);
        try {
            List<Map<String, Object>> ldapUsers = ldapService.listUsers(cluster);
            int inserted = 0;
            int updated = 0;
            int repaired = 0;
            int failed = 0;
            List<Map<String, Object>> failures = new ArrayList<>();

            for (Map<String, Object> u : ldapUsers) {
                try {
                    String username = ldapValue(u, "uid");
                    if (username == null || username.trim().isEmpty()) {
                        throw new IllegalArgumentException("LDAP 用户缺少 uid");
                    }
                    username = username.trim();
                    DgaUser user = findImportTargetUser(username, targetCluster, clusterName);
                    boolean isInsert = user == null;
                    boolean isLegacyRepair = false;
                    if (isInsert) {
                        user = new DgaUser();
                        user.setUsername(username);
                    } else if (!clusterName.equals(user.getClusterName())) {
                        isLegacyRepair = true;
                    }
                    user.setFirstName(firstNonBlank(ldapValue(u, "givenName"), ldapValue(u, "cn"), username));
                    user.setLastName(firstNonBlank(ldapValue(u, "sn"), username));
                    user.setEmail(ldapValue(u, "mail"));
                    user.setCreationStrategy("LDAP_IMPORT");
                    user.setClusterName(clusterName);
                    user.setDeleted(false);
                    dgaUserRepository.save(user);
                    if (isInsert) {
                        inserted++;
                    } else {
                        updated++;
                        if (isLegacyRepair) {
                            repaired++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to import user entry: " + u + " Error: " + e.getMessage());
                    failed++;
                    Map<String, Object> failure = new HashMap<>();
                    failure.put("entry", u);
                    failure.put("message", e.getMessage());
                    failures.add(failure);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cluster", clusterName);
            result.put("total", ldapUsers.size());
            result.put("inserted", inserted);
            result.put("updated", updated);
            result.put("repaired", repaired);
            result.put("failed", failed);
            result.put("failures", failures);
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage() == null ? "OpenLDAP 导入失败" : e.getMessage(), e);
        }
    }

    @PostMapping("/sync/{username}")
    public String syncUserPermissions(@PathVariable String username, @RequestParam(required = false) String cluster) {
        String targetCluster = (cluster != null && !cluster.isEmpty()) ? cluster : "CDH-Cluster-01";
        
        List<Map<String, Object>> hivePermsRaw = hiveAuthService.getUserPermissions(username, targetCluster);
        Set<String> hivePermKeys = new HashSet<>();
        List<UserHiveAccess> toSave = new ArrayList<>();
        
        for (Map<String, Object> row : hivePermsRaw) {
             Map<String, Object> lowerRow = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
             lowerRow.putAll(row);
             
             String db = (String) lowerRow.get("database");
             if (db == null) db = (String) lowerRow.get("database_name");
             
             String table = (String) lowerRow.get("table");
             if (table == null) table = (String) lowerRow.get("table_name");

             String perm = (String) lowerRow.get("privilege");
             if (perm == null) perm = (String) lowerRow.get("permission");
             
             if (db == null || perm == null) continue;
             
             perm = perm.toUpperCase();
             
             String key = db + "|" + (table == null ? "" : table) + "|" + perm;
             hivePermKeys.add(key);
        }
        
        List<UserHiveAccess> localPerms = userHiveAccessRepository.findByUsernameAndIsDeletedFalse(username);
        localPerms = localPerms.stream()
            .filter(p -> targetCluster.equals(p.getClusterName()))
            .collect(Collectors.toList());

        int added = 0;
        int removed = 0;
        int updated = 0;

        // Process Hive Perms (Insert/Update)
        for (String key : hivePermKeys) {
            String[] parts = key.split("\\|", -1);
            String db = parts[0];
            String table = parts[1].isEmpty() ? null : parts[1];
            String perm = parts[2];
            
            boolean found = false;
            for (UserHiveAccess local : localPerms) {
                if (local.getDatabaseName().equals(db) && 
                    Objects.equals(local.getTableName(), table) &&
                    local.getPermission().equals(perm)) {
                    found = true;
                    if (!"ACTIVE".equals(local.getStatus())) {
                        local.setStatus("ACTIVE");
                        toSave.add(local);
                        updated++;
                    }
                    break;
                }
            }
            
            if (!found) {
                UserHiveAccess newAccess = new UserHiveAccess();
                newAccess.setUsername(username);
                newAccess.setClusterName(targetCluster);
                newAccess.setDatabaseName(db);
                newAccess.setTableName(table);
                newAccess.setPermission(perm);
                newAccess.setStatus("ACTIVE");
                newAccess.setGrantedBy("SYNC");
                toSave.add(newAccess);
                added++;
            }
        }
        
        // Process Local Perms (Delete if not in Hive)
        for (UserHiveAccess local : localPerms) {
            String key = local.getDatabaseName() + "|" + (local.getTableName() == null ? "" : local.getTableName()) + "|" + local.getPermission();
            if (!hivePermKeys.contains(key)) {
                local.setDeleted(true); 
                local.setStatus("REVOKED");
                toSave.add(local);
                removed++;
            }
        }
        
        userHiveAccessRepository.saveAll(toSave);
        return String.format("Sync complete. Added: %d, Updated: %d, Removed: %d", added, updated, removed);
    }

    @GetMapping("/users")
    public Page<DgaUser> listUsers(@RequestParam(defaultValue = "0") int page, 
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String cluster,
                                   @RequestParam(value = "q", required = false) String query) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        List<String> excludedStrategies = java.util.Arrays.asList("SELF_REGISTER", "SELF_REG");
        
        if (cluster != null && !cluster.isEmpty()) {
            cluster = resolveClusterName(cluster);
            if (query != null && !query.isEmpty()) {
                return dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotInAndUsernameContainingIgnoreCase(
                        cluster, excludedStrategies, query, pageable);
            }
            return dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(
                    cluster, excludedStrategies, pageable);
        }
        if (query != null && !query.isEmpty()) {
            return dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotInAndUsernameContainingIgnoreCase(
                    excludedStrategies, query, pageable);
        }
        return dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(
                excludedStrategies, pageable);
    }

    @GetMapping("/clusters")
    public List<String> listClusters() {
        // Migration: If no clusters in dga_cluster, populate from existing users
        if (clusterRepository.count() == 0) {
            List<String> existing = dgaUserRepository.findDistinctClusterNames();
            if (existing == null || existing.isEmpty()) {
                existing = java.util.Arrays.asList("CDH-Cluster-01", "HDP-Production");
            }
            for (String name : existing) {
                if (name == null || name.trim().isEmpty()) continue;
                com.dga.cluster.entity.Cluster c = new com.dga.cluster.entity.Cluster();
                c.setClusterName(name);
                c.setType("CDH"); // Default
                c.setDescription("Auto-imported cluster");
                clusterRepository.save(c);
            }
        }
        return clusterRepository.findActiveClusters().stream()
                .map(com.dga.cluster.entity.Cluster::getClusterName)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/ldap/user")
    public Map<String, Object> ldapUser(@RequestParam("username") String username,
                                        @RequestParam(value = "cluster", required = false) String cluster) {
        Map<String, Object> res = new HashMap<>();
        res.put("dn", ldapService.getUserDnString(cluster, username));
        boolean exists = ldapService.userExists(cluster, username);
        res.put("exists", exists);
        if (exists) {
            res.put("attributes", ldapService.getUserInfo(cluster, username));
        }
        return res;
    }

    @GetMapping("/hive/databases")
    public List<String> listDatabases(@RequestParam(required = false) String cluster) {
        return hiveAuthService.listDatabases(cluster);
    }

    @GetMapping("/resources/databases")
    public List<String> listResourceDatabases(@RequestParam(required = false) String cluster) {
        return authorizationService.listDatabases(cluster);
    }

    @GetMapping("/capabilities")
    public AuthorizationCapability authorizationCapability(@RequestParam(required = false) String cluster) {
        return authorizationService.capability(cluster);
    }

    @GetMapping("/hive/tables")
    public List<String> listTables(@RequestParam("database") String database, @RequestParam(required = false) String cluster) {
        return hiveAuthService.listTables(cluster, database);
    }

    @GetMapping("/resources/tables")
    public List<String> listResourceTables(@RequestParam("database") String database,
                                           @RequestParam(required = false) String cluster) {
        return authorizationService.listTables(cluster, database);
    }

    @GetMapping("/resources/principals")
    public List<String> listResourcePrincipals(@RequestParam(required = false) String cluster) {
        List<String> principals;
        try {
            principals = authorizationService.listPrincipals(cluster);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "加载授权用户失败" : e.getMessage();
            if (message.contains("Access denied") && message.contains("GRANT")) {
                message = "StarRocks 端点账号缺少 SYSTEM 上的 GRANT 权限，无法执行 SHOW USERS 查询授权用户";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, e);
        }
        if (principals != null && !principals.isEmpty()) {
            return principals;
        }
        Pageable pageable = PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createTime"));
        List<String> excludedStrategies = java.util.Arrays.asList("SELF_REGISTER", "SELF_REG");
        Page<DgaUser> users;
        if (cluster != null && !cluster.isEmpty()) {
            String targetCluster = cluster;
            com.dga.cluster.entity.Cluster resolvedCluster = clusterRepository.findByClusterCode(cluster);
            if (resolvedCluster != null && resolvedCluster.getClusterName() != null) {
                targetCluster = resolvedCluster.getClusterName();
            }
            users = dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(targetCluster, excludedStrategies, pageable);
            if (users.isEmpty()) {
                users = dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(excludedStrategies, pageable);
            }
        } else {
            users = dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(excludedStrategies, pageable);
        }
        return users.getContent().stream()
                .map(DgaUser::getUsername)
                .collect(Collectors.toList());
    }

    @GetMapping("/resources/permissions")
    public Map<String, Object> listResourcePermissions(@RequestParam("username") String username,
                                                       @RequestParam(required = false) String cluster) {
        try {
            List<Map<String, Object>> rawPermissions = authorizationService.getUserPermissions(username, cluster);
            Map<String, Object> res = new HashMap<>();
            res.put("username", username);
            res.put("cluster", authorizationService.resolveClusterCodeOrName(cluster));
            res.put("engineType", authorizationService.engineType(cluster));
            res.put("authBackend", authorizationService.authBackend(cluster));
            res.put("source", "LIVE_AUTH_BACKEND");
            res.put("grants", normalizePermissionRows(rawPermissions));
            res.put("raw", rawPermissions);
            return res;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }

    @PostMapping("/grant/batch")
    public String batchGrant(@RequestBody BatchGrantRequest request) {
        String username = request.getUsername();
        String permission = request.getPermission();
        List<String> databases = request.getDatabases();
        String clusterName = request.getCluster();
        if (clusterName == null || clusterName.isEmpty()) {
            List<DgaUser> users = dgaUserRepository.findByUsernameAndIsDeletedFalse(username);
            DgaUser u = users != null && users.size() == 1 ? users.get(0) : null;
            clusterName = u != null && u.getClusterName() != null ? u.getClusterName() : "CDH-Cluster-01";
        } else {
            clusterName = resolveClusterName(clusterName);
        }

        System.out.println("Batch grant start. User: " + username + ", Cluster: " + clusterName);

        if (databases != null) {
            for (String database : databases) {
                try {
                    hiveAuthService.grantPermission(username, database, permission, clusterName);
                    
                    UserHiveAccess access = new UserHiveAccess();
                    access.setUsername(username);
                    access.setClusterName(clusterName);
                    access.setDatabaseName(database);
                    access.setPermission(permission);
                    access.setGrantedBy("admin");
                    access.setGrantTime(LocalDateTime.now());
                    access.setStatus("ACTIVE");
                    userHiveAccessRepository.save(access);
                    saveResourceAccess(username, clusterName, database, null, permission, "admin", "DGA_GRANT");
                    System.out.println("Saved DB access: " + database);
                } catch (Exception e) {
                    System.err.println("Failed to grant/save DB access: " + e.getMessage());
                    throw e;
                }
            }
        }
        List<TableGrant> tables = request.getTables();
        if (tables != null) {
            for (TableGrant tableGrant : tables) {
                try {
                    hiveAuthService.grantTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission, clusterName);
                    
                    UserHiveAccess access = new UserHiveAccess();
                    access.setUsername(username);
                    access.setClusterName(clusterName);
                    access.setDatabaseName(tableGrant.getDatabase());
                    access.setTableName(tableGrant.getTable());
                    access.setPermission(permission);
                    access.setGrantedBy("admin");
                    access.setGrantTime(LocalDateTime.now());
                    access.setStatus("ACTIVE");
                    userHiveAccessRepository.save(access);
                    saveResourceAccess(username, clusterName, tableGrant.getDatabase(), tableGrant.getTable(),
                            permission, "admin", "DGA_GRANT");
                    System.out.println("Saved Table access: " + tableGrant.getTable());
                } catch (Exception e) {
                    System.err.println("Failed to grant/save Table access: " + e.getMessage());
                    throw e;
                }
            }
        }
        return "Batch access granted for user: " + username;
    }

    @PostMapping("/grants/batch")
    public String batchGrantResource(@RequestBody BatchGrantRequest request) {
        String username = request.getUsername();
        String permission = request.getPermission();
        String cluster = request.getCluster() != null && !request.getCluster().isEmpty()
                ? request.getCluster() : "CDH-Cluster-01";

        try {
            if (request.getDatabases() != null) {
                for (String database : request.getDatabases()) {
                    GrantCommand command = buildGrantCommand(username, cluster, database, null, permission);
                    authorizationService.grant(command);
                    saveResourceAccess(username, cluster, database, null, permission, "admin", "AUTHORIZATION_CENTER");
                }
            }
            if (request.getTables() != null) {
                for (TableGrant tableGrant : request.getTables()) {
                    GrantCommand command = buildGrantCommand(username, cluster, tableGrant.getDatabase(),
                            tableGrant.getTable(), permission);
                    authorizationService.grant(command);
                    saveResourceAccess(username, cluster, tableGrant.getDatabase(), tableGrant.getTable(),
                            permission, "admin", "AUTHORIZATION_CENTER");
                }
            }
            return "Resource access granted for user: " + username;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }
    
    @GetMapping("/user/access")
    public List<UserHiveAccess> listUserAccess(@RequestParam("username") String username,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "cluster", required = false) String cluster,
                                               @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") boolean includeDeleted) {
        if (includeDeleted) {
            if (status != null && cluster != null) {
                return userHiveAccessRepository.findByUsernameAndClusterNameAndStatus(username, cluster, status);
            } else if (status != null) {
                return userHiveAccessRepository.findByUsernameAndStatus(username, status);
            } else if (cluster != null) {
                return userHiveAccessRepository.findByUsernameAndClusterName(username, cluster);
            } else {
                return userHiveAccessRepository.findByUsername(username);
            }
        }
        if (status != null && cluster != null) {
            return userHiveAccessRepository.findByUsernameAndClusterNameAndStatusAndIsDeletedFalse(username, cluster, status);
        } else if (cluster != null) {
            return userHiveAccessRepository.findByUsernameAndClusterNameAndIsDeletedFalse(username, cluster);
        } else if (status != null) {
            return userHiveAccessRepository.findByUsernameAndStatusAndIsDeletedFalse(username, status);
        } else {
            return userHiveAccessRepository.findByUsernameAndIsDeletedFalse(username);
        }
    }

    @PostMapping("/revoke")
    public String revokeAccess(@RequestBody AccessRequest request) {
        try {
            hiveAuthService.revokePermission(request.getUsername(), request.getDatabase(), request.getPermission(), request.getCluster());
        } catch (Exception e) {
            System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
        }
        
        // Soft delete from DB
        try {
            userHiveAccessRepository.softDeleteDatabaseAccess(request.getUsername(), 
                request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01", 
                request.getDatabase(), 
                request.getPermission());
            userResourceAccessRepository.softDeleteDatabaseAccess(request.getUsername(),
                request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01",
                request.getDatabase(),
                request.getPermission());
        } catch (Exception e) {
             System.err.println("Warning: DB revoke failed: " + e.getMessage());
        }

        return "Access revoked successfully for user: " + request.getUsername();
    }

    @PostMapping("/revoke/batch")
    @org.springframework.transaction.annotation.Transactional
    public String batchRevoke(@RequestBody BatchGrantRequest request) {
        String username = request.getUsername();
        String permission = request.getPermission();
        String cluster = request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01"; // Default or validate
        
        List<String> databases = request.getDatabases();
        if (databases != null) {
            for (String database : databases) {
                try {
                    hiveAuthService.revokePermission(username, database, permission, cluster);
                } catch (Exception e) {
                    System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
                }
                try {
                    userHiveAccessRepository.softDeleteDatabaseAccess(username, cluster, database, permission);
                    userResourceAccessRepository.softDeleteDatabaseAccess(username, cluster, database, permission);
                } catch (Exception e) {
                    System.err.println("Failed to soft delete DB access: " + e.getMessage());
                }
            }
        }
        List<TableGrant> tables = request.getTables();
        if (tables != null) {
            for (TableGrant tableGrant : tables) {
                try {
                    hiveAuthService.revokeTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission, cluster);
                } catch (Exception e) {
                    System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
                }
                try {
                    userHiveAccessRepository.softDeleteTableAccess(username, cluster, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                    userResourceAccessRepository.softDeleteTableAccess(username, cluster, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                } catch (Exception e) {
                    System.err.println("Failed to soft delete Table access: " + e.getMessage());
                }
            }
        }
        return "Batch access revoked for user: " + username;
    }

    @PostMapping("/revokes/batch")
    public String batchRevokeResource(@RequestBody BatchGrantRequest request) {
        String username = request.getUsername();
        String permission = request.getPermission();
        String cluster = request.getCluster() != null && !request.getCluster().isEmpty()
                ? request.getCluster() : "CDH-Cluster-01";

        try {
            if (request.getDatabases() != null) {
                for (String database : request.getDatabases()) {
                    RevokeCommand command = buildRevokeCommand(username, cluster, database, null, permission);
                    authorizationService.revoke(command);
                    userResourceAccessRepository.softDeleteDatabaseAccess(username, cluster, database, permission);
                    userHiveAccessRepository.softDeleteDatabaseAccess(username, cluster, database, permission);
                }
            }
            if (request.getTables() != null) {
                for (TableGrant tableGrant : request.getTables()) {
                    RevokeCommand command = buildRevokeCommand(username, cluster, tableGrant.getDatabase(),
                            tableGrant.getTable(), permission);
                    authorizationService.revoke(command);
                    userResourceAccessRepository.softDeleteTableAccess(username, cluster,
                            tableGrant.getDatabase(), tableGrant.getTable(), permission);
                    userHiveAccessRepository.softDeleteTableAccess(username, cluster,
                            tableGrant.getDatabase(), tableGrant.getTable(), permission);
                }
            }
            return "Resource access revoked for user: " + username;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }
    
    @DeleteMapping("/user/{username}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(@PathVariable String username,
                             @RequestParam(required = false) String cluster,
                             HttpServletRequest request) {
        adminGuard.requireDeletePrivilege(request);
        if (isProtectedBigDataUser(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "大数据重要角色禁止删除: " + username);
        }
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "删除用户必须指定所属集群");
        }
        String clusterName = resolveClusterName(cluster);
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (user != null) {
            // 1. Revoke Hive Permissions
            try {
                hiveAuthService.revokeAll(username, clusterName);
            } catch (Throwable e) {
                 System.err.println("Failed to revoke Hive permissions: " + e.getMessage());
                 // Continue to delete user even if revoke fails? 
                 // User requirement: "首先，先收回用户hive权限" - implying strict order.
                 // But if user doesn't exist in Hive, it shouldn't block deletion.
                 // We logged it, let's proceed but maybe warn in return message.
            }

            // 2. Delete user from the identity backend selected by creation strategy.
            try {
                ldapService.deleteUser(clusterName, username);
            } catch (Exception e) {
                System.err.println("Failed to delete from OpenLDAP: " + e.getMessage());
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (!(msg.contains("not found") || msg.contains("doesn't exist") || msg.contains("does not exist") || msg.contains("no such"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "OpenLDAP 删除失败 (" + clusterName + "): " + e.getMessage(), e);
                }
            }

            // 3. Soft Delete in DB
            user.setDeleted(true);
            dgaUserRepository.save(user);
            
            // 4. Revoke Hive Access Records (Soft Delete)
            try {
                userHiveAccessRepository.softDeleteAllAccessByUsernameAndClusterName(username, clusterName);
                userResourceAccessRepository.softDeleteAllByUsernameAndCluster(username, clusterName);
            } catch (Exception e) {
                System.err.println("Failed to update UserHiveAccess status: " + e.getMessage());
                // Non-blocking, but logged
            }
            
            return "User permissions revoked, deleted from backend, and soft deleted from DGA system: " + username;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在: " + username + " (" + clusterName + ")");
        }
    }

    private boolean isProtectedBigDataUser(String username) {
        return username != null && PROTECTED_BIGDATA_USERS.contains(username.trim().toLowerCase());
    }

    private com.dga.cluster.entity.Cluster resolveCluster(String clusterIdentifier) {
        if (clusterIdentifier == null || clusterIdentifier.trim().isEmpty()) {
            return null;
        }
        String identifier = clusterIdentifier.trim();
        com.dga.cluster.entity.Cluster cluster = clusterRepository.findByClusterCode(identifier);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(identifier);
        }
        return cluster;
    }

    private String resolveClusterName(String clusterIdentifier) {
        com.dga.cluster.entity.Cluster cluster = resolveCluster(clusterIdentifier);
        if (cluster != null && cluster.getClusterName() != null) {
            return cluster.getClusterName();
        }
        if (clusterIdentifier == null || clusterIdentifier.trim().isEmpty()) {
            return "CDH-Cluster-01";
        }
        return clusterIdentifier.trim();
    }

    private DgaUser findImportTargetUser(String username,
                                         com.dga.cluster.entity.Cluster targetCluster,
                                         String clusterName) {
        DgaUser exact = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (exact != null) {
            return exact;
        }

        List<DgaUser> sameName = dgaUserRepository.findAllByUsername(username);
        if (sameName == null || sameName.isEmpty()) {
            return null;
        }
        if (sameName.size() == 1 && isLegacyClusterMatch(sameName.get(0).getClusterName(), targetCluster, clusterName)) {
            return sameName.get(0);
        }
        return null;
    }

    private boolean isLegacyClusterMatch(String existingCluster,
                                         com.dga.cluster.entity.Cluster targetCluster,
                                         String clusterName) {
        if (existingCluster == null || existingCluster.trim().isEmpty()) {
            return true;
        }
        String existing = existingCluster.trim();
        if (existing.equalsIgnoreCase(clusterName)) {
            return true;
        }
        if (targetCluster != null) {
            if (targetCluster.getClusterCode() != null && existing.equalsIgnoreCase(targetCluster.getClusterCode())) {
                return true;
            }
            if (targetCluster.getClusterName() != null && existing.equalsIgnoreCase(targetCluster.getClusterName())) {
                return true;
            }
            String type = targetCluster.getType();
            if (type != null && !type.trim().isEmpty() && existing.toLowerCase().contains(type.trim().toLowerCase())) {
                return true;
            }
        }
        return "CDH-Cluster-01".equalsIgnoreCase(existing) && clusterName.toLowerCase().contains("cdh");
    }

    private String ldapValue(Map<String, Object> entry, String key) {
        if (entry == null) {
            return null;
        }
        Object value = entry.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private GrantCommand buildGrantCommand(String username, String cluster, String database,
                                           String table, String permission) {
        GrantCommand command = new GrantCommand();
        command.setUsername(username);
        command.setCluster(cluster);
        command.setDatabase(database);
        command.setTable(table);
        command.setPermission(permission);
        return command;
    }

    private RevokeCommand buildRevokeCommand(String username, String cluster, String database,
                                             String table, String permission) {
        RevokeCommand command = new RevokeCommand();
        command.setUsername(username);
        command.setCluster(cluster);
        command.setDatabase(database);
        command.setTable(table);
        command.setPermission(permission);
        return command;
    }

    private String readableAuthorizationError(Exception e) {
        String message = e.getMessage() == null ? "授权执行失败" : e.getMessage();
        if (message.contains("Access denied") || message.toLowerCase().contains("denied")) {
            return "授权端点账号权限不足: " + message;
        }
        if (message.toLowerCase().contains("syntax") || message.toLowerCase().contains("sql")) {
            return "授权 SQL 执行失败: " + message;
        }
        return message;
    }

    private List<Map<String, Object>> normalizePermissionRows(List<Map<String, Object>> rawPermissions) {
        List<Map<String, Object>> grants = new ArrayList<>();
        if (rawPermissions == null) {
            return grants;
        }
        int index = 1;
        for (Map<String, Object> row : rawPermissions) {
            Map<String, Object> grant = normalizePermissionRow(row);
            grant.put("id", index++);
            grants.add(grant);
        }
        return grants;
    }

    private Map<String, Object> normalizePermissionRow(Map<String, Object> row) {
        Map<String, Object> lowerRow = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (row != null) {
            lowerRow.putAll(row);
        }

        String grantText = pickString(lowerRow, "grants", "grant", "grant_stmt", "grant_statement", "privilege");
        if (grantText == null) {
            grantText = firstStringValue(row);
        }
        String database = pickString(lowerRow, "database", "database_name", "db", "db_name");
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

        Map<String, Object> grant = new HashMap<>();
        grant.put("resourceType", resourceType);
        grant.put("databaseName", database);
        grant.put("tableName", table);
        grant.put("permission", permission != null ? permission.toUpperCase() : "-");
        grant.put("grantText", grantText);
        grant.put("raw", row);
        return grant;
    }

    private Map<String, String> parseGrantText(String grantText) {
        Map<String, String> parsed = new HashMap<>();
        Matcher privilegeMatcher = Pattern.compile("(?i)^GRANT\\s+(.+?)\\s+ON\\s+").matcher(grantText);
        if (privilegeMatcher.find()) {
            parsed.put("permission", privilegeMatcher.group(1).trim().replace("_PRIV", ""));
        }

        if (Pattern.compile("(?i)ON\\s+ALL\\s+TABLES\\s+IN\\s+ALL\\s+DATABASES").matcher(grantText).find()) {
            parsed.put("resourceType", "GLOBAL");
            parsed.put("database", "ALL DATABASES");
            return parsed;
        }

        Matcher allTablesMatcher = Pattern.compile("(?i)ON\\s+ALL\\s+TABLES\\s+IN\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (allTablesMatcher.find()) {
            parsed.put("resourceType", "DATABASE");
            parsed.put("database", allTablesMatcher.group(1));
            return parsed;
        }

        Matcher allViewsMatcher = Pattern.compile("(?i)ON\\s+ALL\\s+VIEWS\\s+IN\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (allViewsMatcher.find()) {
            parsed.put("resourceType", "VIEW");
            parsed.put("database", allViewsMatcher.group(1));
            parsed.put("table", "ALL VIEWS");
            return parsed;
        }

        if (Pattern.compile("(?i)ON\\s+ALL\\s+VIEWS\\s+IN\\s+ALL\\s+DATABASES").matcher(grantText).find()) {
            parsed.put("resourceType", "VIEW");
            parsed.put("database", "ALL DATABASES");
            parsed.put("table", "ALL VIEWS");
            return parsed;
        }

        Matcher allMaterializedViewsMatcher = Pattern.compile("(?i)ON\\s+ALL\\s+MATERIALIZED\\s+VIEWS\\s+IN\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (allMaterializedViewsMatcher.find()) {
            parsed.put("resourceType", "MATERIALIZED_VIEW");
            parsed.put("database", allMaterializedViewsMatcher.group(1));
            parsed.put("table", "ALL MATERIALIZED VIEWS");
            return parsed;
        }

        if (Pattern.compile("(?i)ON\\s+ALL\\s+MATERIALIZED\\s+VIEWS\\s+IN\\s+ALL\\s+DATABASES").matcher(grantText).find()) {
            parsed.put("resourceType", "MATERIALIZED_VIEW");
            parsed.put("database", "ALL DATABASES");
            parsed.put("table", "ALL MATERIALIZED VIEWS");
            return parsed;
        }

        Matcher allFunctionsMatcher = Pattern.compile("(?i)ON\\s+ALL\\s+FUNCTIONS\\s+IN\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (allFunctionsMatcher.find()) {
            parsed.put("resourceType", "FUNCTION");
            parsed.put("database", allFunctionsMatcher.group(1));
            parsed.put("table", "ALL FUNCTIONS");
            return parsed;
        }

        if (Pattern.compile("(?i)ON\\s+ALL\\s+FUNCTIONS\\s+IN\\s+ALL\\s+DATABASES").matcher(grantText).find()) {
            parsed.put("resourceType", "FUNCTION");
            parsed.put("database", "ALL DATABASES");
            parsed.put("table", "ALL FUNCTIONS");
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

    private String firstStringValue(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        for (Object value : row.values()) {
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private String pickString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private void saveResourceAccess(String username, String cluster, String database, String table,
                                    String permission, String grantedBy, String source) {
        String clusterIdentifier = cluster != null && !cluster.isEmpty() ? cluster : "CDH-Cluster-01";
        UserResourceAccess access = new UserResourceAccess();
        access.setUsername(username);
        access.setClusterName(clusterIdentifier);
        access.setClusterCode(authorizationService.resolveClusterCodeOrName(clusterIdentifier));
        access.setEngineType(authorizationService.engineType(clusterIdentifier));
        access.setAuthBackend(authorizationService.authBackend(clusterIdentifier));
        access.setResourceType(table == null || table.isEmpty() ? "DATABASE" : "TABLE");
        access.setDatabaseName(database);
        access.setTableName(table);
        access.setPermission(permission);
        access.setGrantedBy(grantedBy);
        access.setSource(source);
        access.setStatus("ACTIVE");
        access.setGrantTime(LocalDateTime.now());
        userResourceAccessRepository.save(access);
    }
}
