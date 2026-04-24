package com.dga.access.controller;

import com.dga.access.dto.AccessRequest;
import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.CreateUserRequest;
import com.dga.access.dto.TableGrant;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.DgaUserRepository;
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
        // Check if user exists in DB first
        if (dgaUserRepository.existsByUsername(request.getUsername())) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "User " + request.getUsername() + " already exists in system.");
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
        if (!dgaUserRepository.existsByUsername(request.getUsername())) {
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
            user.setClusterName(request.getCluster());
            dgaUserRepository.save(user);
        } else {
             // If user exists (e.g. re-registering or different strategy), update it?
             // For now, let's just ignore or maybe update the strategy/password if needed.
             // But requirement says "IPA registered account, password not saved". 
             // So if it exists, we might want to ensure password is saved if it wasn't before.
             DgaUser user = dgaUserRepository.findByUsername(request.getUsername());
             if (user.getPassword() == null && request.getPassword() != null) {
                 user.setPassword(passwordEncoder.encode(request.getPassword()));
                 user.setClusterName(request.getCluster());
                 dgaUserRepository.save(user);
             }
        }

        return resultMsg;
    }
    
    @PostMapping("/import")
    public String importUsers() {
        try {
            List<Map<String, Object>> ipaUsers = ipaHttpService.listUsers();
            int count = 0;
            int failed = 0;
            for (Map<String, Object> u : ipaUsers) {
                try {
                    Object uidObj = u.get("uid");
                    if (uidObj instanceof List && !((List<?>) uidObj).isEmpty()) {
                        String username = (String) ((List<?>) uidObj).get(0);
                        if (!dgaUserRepository.existsByUsername(username)) {
                            DgaUser newUser = new DgaUser();
                            newUser.setUsername(username);
                            newUser.setCreationStrategy("IPA_IMPORT");
                            newUser.setClusterName("CDH");
                            
                            // Try to get name
                            Object givenName = u.get("givenname");
                            if (givenName instanceof List && !((List<?>) givenName).isEmpty()) {
                                newUser.setFirstName((String)((List<?>) givenName).get(0));
                            }
                            Object sn = u.get("sn");
                            if (sn instanceof List && !((List<?>) sn).isEmpty()) {
                                newUser.setLastName((String)((List<?>) sn).get(0));
                            }
                            
                            dgaUserRepository.save(newUser);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to import user entry: " + u + " Error: " + e.getMessage());
                    failed++;
                }
            }
            return "Import complete. Success: " + count + ", Failed: " + failed;
        } catch (Exception e) {
            e.printStackTrace();
            return "Import failed: " + e.getMessage();
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
            users = dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(cluster, excludedStrategies, pageable);
        } else {
            users = dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(excludedStrategies, pageable);
        }
        return users.getContent().stream()
                .map(DgaUser::getUsername)
                .collect(Collectors.toList());
    }

    @PostMapping("/grant/batch")
    public String batchGrant(@RequestBody BatchGrantRequest request) {
        String username = request.getUsername();
        String permission = request.getPermission();
        List<String> databases = request.getDatabases();
        String clusterName = request.getCluster();
        if (clusterName == null || clusterName.isEmpty()) {
            DgaUser u = dgaUserRepository.findByUsername(username);
            clusterName = u != null && u.getClusterName() != null ? u.getClusterName() : "CDH-Cluster-01";
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
    public String deleteUser(@PathVariable String username) {
        if (isProtectedBigDataUser(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "大数据重要角色禁止删除: " + username);
        }
        DgaUser user = dgaUserRepository.findByUsername(username);
        if (user != null) {
            // 1. Revoke Hive Permissions
            try {
                hiveAuthService.revokeAll(username, user.getClusterName());
            } catch (Throwable e) {
                 System.err.println("Failed to revoke Hive permissions: " + e.getMessage());
                 // Continue to delete user even if revoke fails? 
                 // User requirement: "首先，先收回用户hive权限" - implying strict order.
                 // But if user doesn't exist in Hive, it shouldn't block deletion.
                 // We logged it, let's proceed but maybe warn in return message.
            }

            // 2. Delete Linux user via FreeIPA (HTTP)
            String strategy = user.getCreationStrategy();
            try {
                if (ipaHttpService.userExists(username)) {
                    ipaHttpService.deleteUser(username);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete from backend: " + e.getMessage());
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (!(msg.contains("not found") || msg.contains("doesn't exist") || msg.contains("does not exist") || msg.contains("no such"))) {
                    throw new RuntimeException("Failed to delete user from backend (" + strategy + "): " + e.getMessage());
                }
            }

            // 3. Soft Delete in DB
            user.setDeleted(true);
            dgaUserRepository.save(user);
            
            // 4. Revoke Hive Access Records (Soft Delete)
            try {
                userHiveAccessRepository.softDeleteAllAccessByUsername(username);
                userResourceAccessRepository.softDeleteAllByUsername(username);
            } catch (Exception e) {
                System.err.println("Failed to update UserHiveAccess status: " + e.getMessage());
                // Non-blocking, but logged
            }
            
            return "User permissions revoked, deleted from backend, and soft deleted from DGA system: " + username;
        } else {
            throw new RuntimeException("User not found: " + username);
        }
    }

    private boolean isProtectedBigDataUser(String username) {
        return username != null && PROTECTED_BIGDATA_USERS.contains(username.trim().toLowerCase());
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
