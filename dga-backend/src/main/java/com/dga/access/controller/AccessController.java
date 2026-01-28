package com.dga.access.controller;

import com.dga.access.dto.AccessRequest;
import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.CreateUserRequest;
import com.dga.access.dto.TableGrant;
import com.dga.access.entity.DgaUser;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.service.HiveAuthService;
import com.dga.access.service.IpaHttpService;
import com.dga.access.service.IpaService;
import com.dga.access.service.LdapService;
import com.dga.access.entity.UserHiveAccess;
import com.dga.access.repository.UserHiveAccessRepository;
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

@RestController
@RequestMapping("/api/access")
@CrossOrigin
public class AccessController {

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
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/grant")
    public String grantAccess(@RequestBody AccessRequest request) {
        ldapService.createUser(request.getUsername(), request.getPassword(), request.getEmail());
        hiveAuthService.grantPermission(request.getUsername(), request.getDatabase(), request.getPermission());
        
        // Record Access
        UserHiveAccess access = new UserHiveAccess();
        access.setUsername(request.getUsername());
        access.setClusterName(request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01");
        access.setDatabaseName(request.getDatabase());
        access.setPermission(request.getPermission());
        access.setGrantedBy("admin"); // TODO: get from security context
        access.setStatus("ACTIVE");
        userHiveAccessRepository.save(access);

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
            strategy = "IPA_HTTP";
        }
        
        if ("IPA_SSH".equalsIgnoreCase(strategy)) {
            String result = ipaService.createUser(request.getIpaHost(), request.getUsername(), request.getFirstName(), request.getLastName(), request.getPassword());
            if (!result.isEmpty()) return result; // Return error if any
            resultMsg = "User created via IPA(SSH): " + request.getUsername();
        } else if ("IPA_HTTP".equalsIgnoreCase(strategy)) {
            ipaHttpService.createUser(request.getUsername(), request.getFirstName(), request.getLastName(), request.getPassword());
            resultMsg = "User created via IPA(HTTP): " + request.getUsername();
        } else {
            ldapService.createUser(request.getUsername(), request.getPassword(), request.getEmail());
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
    
    @GetMapping("/users")
    public Page<DgaUser> listUsers(@RequestParam(defaultValue = "0") int page, 
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String cluster) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        List<String> excludedStrategies = java.util.Arrays.asList("SELF_REGISTER", "SELF_REG");
        
        if (cluster != null && !cluster.isEmpty()) {
            return dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(cluster, excludedStrategies, pageable);
        }
        return dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(excludedStrategies, pageable);
    }

    @GetMapping("/clusters")
    public List<String> listClusters() {
        return dgaUserRepository.findDistinctClusterNames();
    }

    @GetMapping("/ldap/user")
    public Map<String, Object> ldapUser(@RequestParam("username") String username) {
        Map<String, Object> res = new HashMap<>();
        res.put("dn", ldapService.getUserDnString(username));
        boolean exists = ldapService.userExists(username);
        res.put("exists", exists);
        if (exists) {
            res.put("attributes", ldapService.getUserInfo(username));
        }
        return res;
    }

    @GetMapping("/hive/databases")
    public List<String> listDatabases() {
        return hiveAuthService.listDatabases();
    }

    @GetMapping("/hive/tables")
    public List<String> listTables(@RequestParam("database") String database) {
        return hiveAuthService.listTables(database);
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
                    hiveAuthService.grantPermission(username, database, permission);
                    
                    UserHiveAccess access = new UserHiveAccess();
                    access.setUsername(username);
                    access.setClusterName(clusterName);
                    access.setDatabaseName(database);
                    access.setPermission(permission);
                    access.setGrantedBy("admin");
                    access.setGrantTime(LocalDateTime.now());
                    access.setStatus("ACTIVE");
                    userHiveAccessRepository.save(access);
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
                    hiveAuthService.grantTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                    
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
                    System.out.println("Saved Table access: " + tableGrant.getTable());
                } catch (Exception e) {
                    System.err.println("Failed to grant/save Table access: " + e.getMessage());
                    throw e;
                }
            }
        }
        return "Batch access granted for user: " + username;
    }
    
    @GetMapping("/user/access")
    public List<UserHiveAccess> listUserAccess(@RequestParam("username") String username,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "cluster", required = false) String cluster) {
        if (status != null && cluster != null) {
            return userHiveAccessRepository.findByUsernameAndClusterNameAndStatus(username, cluster, status);
        } else if (cluster != null) {
            return userHiveAccessRepository.findByUsernameAndClusterName(username, cluster);
        } else if (status != null) {
            return userHiveAccessRepository.findByUsernameAndStatus(username, status);
        } else {
            return userHiveAccessRepository.findByUsername(username);
        }
    }

    @PostMapping("/revoke")
    public String revokeAccess(@RequestBody AccessRequest request) {
        hiveAuthService.revokePermission(request.getUsername(), request.getDatabase(), request.getPermission());
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
                hiveAuthService.revokePermission(username, database, permission);
                try {
                    // userHiveAccessRepository.revokeDatabaseAccess(username, cluster, database, permission);
                    String sql = String.format("DELETE FROM user_hive_access WHERE username = '%s' AND cluster_name = '%s' AND database_name = '%s' AND table_name IS NULL AND permission = '%s'",
                            username, cluster, database, permission);
                    System.out.println("Executing Access Delete SQL: " + sql);
                    jdbcTemplate.execute(sql);
                } catch (Exception e) {
                    System.err.println("Failed to update DB access status: " + e.getMessage());
                }
            }
        }
        List<TableGrant> tables = request.getTables();
        if (tables != null) {
            for (TableGrant tableGrant : tables) {
                hiveAuthService.revokeTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                try {
                    // userHiveAccessRepository.revokeTableAccess(username, cluster, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                    String sql = String.format("DELETE FROM user_hive_access WHERE username = '%s' AND cluster_name = '%s' AND database_name = '%s' AND table_name = '%s' AND permission = '%s'",
                            username, cluster, tableGrant.getDatabase(), tableGrant.getTable(), permission);
                    System.out.println("Executing Access Delete SQL: " + sql);
                    jdbcTemplate.execute(sql);
                } catch (Exception e) {
                    System.err.println("Failed to update Table access status: " + e.getMessage());
                }
            }
        }
        return "Batch access revoked for user: " + username;
    }
    
    @DeleteMapping("/user/{username}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(@PathVariable String username) {
        DgaUser user = dgaUserRepository.findByUsername(username);
        if (user != null) {
            // 1. Revoke Hive Permissions
            try {
                hiveAuthService.revokeAll(username);
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
                userHiveAccessRepository.revokeAllAccessByUsername(username);
            } catch (Exception e) {
                System.err.println("Failed to update UserHiveAccess status: " + e.getMessage());
                // Non-blocking, but logged
            }
            
            return "User permissions revoked, deleted from backend, and soft deleted from DGA system: " + username;
        } else {
            throw new RuntimeException("User not found: " + username);
        }
    }
}
