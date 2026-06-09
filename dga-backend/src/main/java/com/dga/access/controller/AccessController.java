package com.dga.access.controller;

import com.dga.access.dto.AccessRequest;
import com.dga.access.dto.AuthRoleAssignmentRequest;
import com.dga.access.dto.AuthRoleImportRequest;
import com.dga.access.dto.AuthRoleImportResult;
import com.dga.access.dto.AuthRolePermissionRequest;
import com.dga.access.dto.AuthRoleRequest;
import com.dga.access.dto.AuthRoleView;
import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRoleSnapshot;
import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.BatchRoleAssignmentRequest;
import com.dga.access.dto.BatchRoleAssignmentResult;
import com.dga.access.dto.CreateUserRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionPreview;
import com.dga.access.dto.HistoricalPermissionAdoptionPreviewRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionResult;
import com.dga.access.dto.StarRocksPermissionInventoryRequest;
import com.dga.access.dto.TableGrant;
import com.dga.access.entity.AuthRoleAssignmentAudit;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.security.CurrentUser;
import com.dga.access.repository.AuthRoleAssignmentAuditRepository;
import com.dga.access.repository.AuthRolePermissionRepository;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.service.AdminGuard;
import com.dga.access.service.AuthRoleService;
import com.dga.access.service.DgaUserSchemaService;
import com.dga.access.service.HistoricalPermissionAdoptionService;
import com.dga.access.service.HiveAuthService;
import com.dga.access.service.IpaHttpService;
import com.dga.access.service.IpaService;
import com.dga.access.service.LdapService;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.access.service.authorization.AuthorizationSupport;
import com.dga.access.service.authorization.AuthorizationCapability;
import com.dga.access.service.authorization.GrantCommand;
import com.dga.access.service.authorization.RevokeCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dga.access.entity.UserHiveAccess;
import com.dga.access.repository.UserHiveAccessRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/access")
@CrossOrigin
@Tag(name = "权限管理", description = "OpenLDAP 用户管理、授权查询、权限授予回收与同步")
@SecurityRequirement(name = "bearerAuth")
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
    private AuthRolePermissionRepository authRolePermissionRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private com.dga.cluster.repository.ClusterRepository clusterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private DgaUserSchemaService dgaUserSchemaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRoleService authRoleService;

    @Autowired
    private AuthRoleAssignmentAuditRepository authRoleAssignmentAuditRepository;

    @Autowired
    private HistoricalPermissionAdoptionService historicalPermissionAdoptionService;

    @PostMapping("/grant")
    public String grantAccess(@RequestBody AccessRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行授权操作");
        String operator = currentOperator();
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

        UserHiveAccess access = new UserHiveAccess();
        access.setUsername(request.getUsername());
        access.setClusterName(request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01");
        access.setDatabaseName(request.getDatabase());
        access.setPermission(request.getPermission());
        access.setGrantedBy(operator);
        access.setStatus("ACTIVE");
        userHiveAccessRepository.save(access);
        saveResourceAccess(request.getUsername(), request.getCluster(), request.getDatabase(), null,
                request.getPermission(), operator, "DGA_GRANT");

        return "Access granted successfully for user: " + request.getUsername();
    }

    @PostMapping("/roles")
    @Operation(summary = "创建 RBAC 角色", description = "创建或更新 DGA RBAC 角色，并同步创建后端角色。")
    public AuthRoleView createRole(@RequestBody AuthRoleRequest request,
                                   HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可创建 RBAC 角色");
        return authRoleService.createOrUpdateRole(request);
    }

    @PutMapping("/roles/{roleCode}")
    @Operation(summary = "更新 RBAC 角色", description = "更新角色元数据和状态。")
    public AuthRoleView updateRole(@PathVariable String roleCode,
                                   @RequestBody AuthRoleRequest request,
                                   HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可更新 RBAC 角色");
        request.setRoleCode(roleCode);
        return authRoleService.createOrUpdateRole(request);
    }

    @GetMapping("/roles")
    @Operation(summary = "查询 RBAC 角色目录", description = "按集群和授权后端过滤 DGA 角色目录。")
    public List<AuthRoleView> listRoles(@RequestParam(required = false) String cluster,
                                        @RequestParam(required = false) String authBackend) {
        return authRoleService.listRoles(cluster, authBackend);
    }

    @GetMapping("/roles/backend")
    @Operation(summary = "查询授权后端已有角色", description = "只读盘点授权后端已有角色、权限和可发现的组绑定。")
    public List<BackendRoleSnapshot> listBackendRoles(@RequestParam String cluster,
                                                      @RequestParam(required = false) String authBackend,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) List<String> roleCodes,
                                                      @RequestParam(defaultValue = "true") boolean includePermissions,
                                                      @RequestParam(defaultValue = "true") boolean includeAssignments,
                                                      HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可同步后端角色");
        BackendRoleInventoryRequest inventoryRequest = new BackendRoleInventoryRequest();
        inventoryRequest.setCluster(cluster);
        inventoryRequest.setAuthBackend(authBackend);
        inventoryRequest.setKeyword(keyword);
        inventoryRequest.setRoleCodes(roleCodes);
        inventoryRequest.setIncludePermissions(includePermissions);
        inventoryRequest.setIncludeAssignments(includeAssignments);
        return authRoleService.listBackendRoles(inventoryRequest);
    }

    @PostMapping("/roles/backend/import")
    @Operation(summary = "接管授权后端已有角色", description = "把授权后端已有角色写入 DGA 本地元数据，不修改后端授权。")
    public AuthRoleImportResult importBackendRoles(@RequestBody AuthRoleImportRequest request,
                                                   HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可接管后端角色");
        return authRoleService.importBackendRoles(request, currentOperator());
    }

    @PostMapping("/roles/{roleCode}/historical-adoption/preview")
    @Operation(summary = "预览历史用户权限接管", description = "对比历史用户 live 权限与 DGA 角色范围，不修改后端授权。")
    public HistoricalPermissionAdoptionPreview previewHistoricalPermissionAdoption(
            @PathVariable String roleCode,
            @RequestBody HistoricalPermissionAdoptionPreviewRequest request,
            HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可预览历史权限接管");
        return historicalPermissionAdoptionService.preview(roleCode, request);
    }

    @PostMapping("/roles/{roleCode}/historical-adoption")
    @Operation(summary = "接管历史用户权限", description = "把历史用户已有后端权限写入 DGA 本地记录，不下发或回收后端权限。")
    public HistoricalPermissionAdoptionResult adoptHistoricalPermissions(
            @PathVariable String roleCode,
            @RequestBody HistoricalPermissionAdoptionRequest request,
            HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可接管历史权限");
        return historicalPermissionAdoptionService.adopt(roleCode, request, currentOperator());
    }

    @PostMapping("/roles/{roleCode}/permissions")
    @Operation(summary = "给角色添加权限", description = "把资源权限挂到角色并下发到真实授权后端。")
    public AuthRoleView addRolePermission(@PathVariable String roleCode,
                                          @RequestBody AuthRolePermissionRequest request,
                                          HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可维护 RBAC 角色权限");
        return authRoleService.addPermission(roleCode, request);
    }

    @DeleteMapping("/roles/{roleCode}/permissions/{permissionId}")
    @Operation(summary = "删除角色权限", description = "从角色权限范围中删除指定资源权限，并尽力同步回收后端角色权限。")
    public AuthRoleView deleteRolePermission(@PathVariable String roleCode,
                                             @PathVariable Long permissionId,
                                             HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可维护 RBAC 角色权限");
        return authRoleService.deletePermission(roleCode, permissionId);
    }

    @PostMapping("/roles/{roleCode}/assignments")
    @Operation(summary = "分配角色", description = "把角色分配给用户或组，并尽力同步到后端。")
    public AuthRoleView addRoleAssignment(@PathVariable String roleCode,
                                          @RequestBody AuthRoleAssignmentRequest request,
                                          HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可分配 RBAC 角色");
        return authRoleService.addAssignment(roleCode, request);
    }

    @PostMapping("/roles/{roleCode}/assignments/batch/dry-run")
    @Operation(summary = "批量角色绑定 dry-run", description = "预览一批用户补充角色绑定的影响范围，不修改用户或授权记录。")
    public BatchRoleAssignmentResult dryRunBatchRoleAssignments(@PathVariable String roleCode,
                                                               @RequestBody BatchRoleAssignmentRequest request,
                                                               HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可批量分配 RBAC 角色");
        return authRoleService.dryRunBatchAssignments(roleCode, request);
    }

    @PostMapping("/roles/{roleCode}/assignments/batch")
    @Operation(summary = "批量分配角色", description = "给一批历史用户补充角色绑定记录，并逐用户返回同步状态。")
    public BatchRoleAssignmentResult batchRoleAssignments(@PathVariable String roleCode,
                                                         @RequestBody BatchRoleAssignmentRequest request,
                                                         HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可批量分配 RBAC 角色");
        return authRoleService.batchAssignUsers(roleCode, request);
    }

    @GetMapping("/roles/{roleCode}/effective-permissions")
    @Operation(summary = "查询角色有效权限", description = "查询角色权限和当前绑定对象。")
    public AuthRoleView roleEffectivePermissions(@PathVariable String roleCode) {
        return authRoleService.effectivePermissions(roleCode);
    }

    @GetMapping("/roles/{roleCode}/permissions/{permissionId}/tables")
    @Operation(summary = "查询角色库级权限下的可选表", description = "列出指定角色库级权限覆盖的数据库中的所有表，供前端表级子集授权时选择。")
    public List<String> listRolePermissionTables(@PathVariable String roleCode,
                                                 @PathVariable Long permissionId,
                                                 @RequestParam(required = false) String cluster,
                                                 @RequestParam(required = false) String authBackend,
                                                 HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询角色权限表列表");
        AuthRolePermission rolePermission = authRolePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "角色权限不存在: " + permissionId));
        if (!roleCode.equals(rolePermission.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "权限不属于该角色");
        }
        if (!"DATABASE".equalsIgnoreCase(rolePermission.getResourceType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持库级权限的表展开");
        }
        String resolvedCluster = firstNonBlank(cluster, rolePermission.getCluster());
        String resolvedAuthBackend = firstNonBlank(authBackend, rolePermission.getAuthBackend());
        return authorizationService.listTables(resolvedCluster, rolePermission.getDatabaseName(), resolvedAuthBackend);
    }

    @DeleteMapping("/roles/{roleCode}/assignments")
    @Operation(summary = "回收角色绑定", description = "按主体类型和主体名称回收指定角色绑定。")
    public AuthRoleView revokeRoleAssignment(@PathVariable String roleCode,
                                             @RequestParam String subjectType,
                                             @RequestParam String subjectName,
                                             @RequestParam(required = false) String authBackend,
                                             HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可回收 RBAC 角色绑定");
        return authRoleService.revokeAssignment(roleCode, subjectType, subjectName, authBackend);
    }

    @DeleteMapping("/roles/{roleCode}")
    @Operation(summary = "删除 RBAC 角色", description = "从 DGA 角色库软删除角色及其本地权限/绑定记录，不自动回收后端权限。")
    public void deleteRole(@PathVariable String roleCode, HttpServletRequest request) {
        adminGuard.requireDeletePrivilege(request);
        authRoleService.deleteRole(roleCode);
    }

    @PostMapping("/user")
    public String createUser(@RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可创建权限用户");
        dgaUserSchemaService.ensureClusterScopedUsernameConstraint();
        String clusterName = resolveClusterName(request.getCluster());
        com.dga.cluster.entity.Cluster targetCluster = resolveCluster(request.getCluster());
        String clusterType = targetCluster != null && targetCluster.getType() != null ? targetCluster.getType().toUpperCase() : "";
        String sqlEngine = resolveSqlAuthEngine(request.getCluster(), targetCluster);
        boolean sqlAuthUser = sqlEngine != null;
        String userType = normalizeGovernanceUserType(request.getUserType());
        validateUserExpiry(userType, request.getExpiresAt());
        DgaUser existingUser = dgaUserRepository.findByUsernameAndClusterName(request.getUsername(), clusterName);
        // Check if an active user already exists in DB first
        if (existingUser != null && !Boolean.TRUE.equals(existingUser.getDeleted())) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "User " + request.getUsername() + " already exists in cluster " + clusterName + ".");
        }

        String strategy = request.getCreationStrategy();
        String resultMsg = "User created: " + request.getUsername();
        if (sqlAuthUser) {
            validateSqlAuthUsername(request.getUsername(), sqlEngine);
            try {
                authorizationService.createUser(request.getCluster(), request.getUsername(), request.getPassword());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
            }
            strategy = sqlEngine;
            resultMsg = strategy + " user created: " + request.getUsername();
        } else if (strategy == null || strategy.isEmpty() || strategy.toUpperCase().startsWith("SELF")) {
            strategy = "OPENLDAP";
        }
        
        if (sqlAuthUser) {
        } else if ("IPA_SSH".equalsIgnoreCase(strategy)) {
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
            boolean posixAccount = !"LDAP_ONLY".equalsIgnoreCase(request.getAccountMode());
            ldapService.createUser(request.getCluster(), request.getUsername(), request.getPassword(),
                    request.getEmail(), request.getGidNumber(), request.getGroupName(), posixAccount);
            strategy = "OPENLDAP";
            resultMsg = posixAccount
                    ? "System account created via OpenLDAP: " + request.getUsername()
                    : "LDAP identity created via OpenLDAP: " + request.getUsername();
        }

        // Persist to MySQL
        if (existingUser == null) {
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
            user.setUserType(userType);
            user.setExpiresAt(request.getExpiresAt());
            user.setDeleted(false);
            if ("OPENLDAP".equalsIgnoreCase(strategy) || "LDAP".equalsIgnoreCase(strategy)) {
                syncDgaUserFromLdap(user, request.getCluster(), request.getUsername());
            }
            dgaUserRepository.save(user);
        } else {
             DgaUser user = existingUser;
             if (user.getPassword() == null && request.getPassword() != null) {
                 user.setPassword(passwordEncoder.encode(request.getPassword()));
             } else if (request.getPassword() != null) {
                 user.setPassword(passwordEncoder.encode(request.getPassword()));
             }
             user.setFirstName(request.getFirstName());
             user.setLastName(request.getLastName());
             user.setEmail(request.getEmail());
             user.setCreationStrategy(strategy);
             user.setClusterName(clusterName);
             user.setUserType(userType);
             user.setExpiresAt(request.getExpiresAt());
             user.setDeleted(false);
             if ("OPENLDAP".equalsIgnoreCase(strategy) || "LDAP".equalsIgnoreCase(strategy)) {
                 syncDgaUserFromLdap(user, request.getCluster(), request.getUsername());
             }
             dgaUserRepository.save(user);
        }

        return resultMsg;
    }

    @GetMapping("/ldap-groups")
    @Operation(summary = "查询 LDAP 用户组", description = "返回指定集群 LDAP 中可用于创建系统账号的 posixGroup 列表。")
    public List<Map<String, Object>> listLdapGroups(@RequestParam(required = false) String cluster,
                                                    HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询 LDAP 用户组");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再查询 LDAP 用户组");
        }
        return ldapService.listPosixGroups(cluster);
    }

    @GetMapping("/ldap-groups/{groupName}")
    @Operation(summary = "查询 LDAP 用户组详情", description = "返回指定 posixGroup 的 DN、gidNumber、描述和 memberUid。")
    public Map<String, Object> getLdapGroup(@PathVariable String groupName,
                                            @RequestParam(required = false) String cluster,
                                            HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询 LDAP 用户组");
        requireCluster(cluster, "请选择所属集群后再查询 LDAP 用户组");
        return ldapService.getPosixGroup(cluster, groupName);
    }

    @PostMapping("/ldap-groups")
    @Operation(summary = "创建 LDAP 用户组", description = "创建 OpenLDAP posixGroup，可指定 gidNumber 和描述。")
    public Map<String, Object> createLdapGroup(@RequestParam(required = false) String cluster,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可创建 LDAP 用户组");
        requireCluster(cluster, "请选择所属集群后再创建 LDAP 用户组");
        String name = stringBodyValue(body, "name");
        Long gidNumber = longBodyValue(body, "gidNumber");
        String description = stringBodyValue(body, "description");
        return ldapService.createPosixGroup(cluster, name, gidNumber, description);
    }

    @PutMapping("/ldap-groups/{groupName}")
    @Operation(summary = "更新 LDAP 用户组", description = "更新 posixGroup 的 gidNumber、描述和 memberUid。")
    public Map<String, Object> updateLdapGroup(@PathVariable String groupName,
                                               @RequestParam(required = false) String cluster,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可更新 LDAP 用户组");
        requireCluster(cluster, "请选择所属集群后再更新 LDAP 用户组");
        Long gidNumber = longBodyValue(body, "gidNumber");
        String description = body != null && body.containsKey("description") ? stringBodyValue(body, "description") : null;
        List<String> memberUids = null;
        if (body != null && body.get("members") instanceof List) {
            memberUids = new ArrayList<>();
            for (Object item : (List<?>) body.get("members")) {
                if (item != null) {
                    memberUids.add(String.valueOf(item));
                }
            }
        }
        return ldapService.updatePosixGroup(cluster, groupName, gidNumber, description, memberUids);
    }

    @PutMapping("/ldap-groups/{groupName}/members")
    @Operation(summary = "更新 LDAP 用户组成员", description = "批量替换 posixGroup 的 memberUid 列表。")
    public Map<String, Object> updateLdapGroupMembers(@PathVariable String groupName,
                                                      @RequestParam(required = false) String cluster,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可更新 LDAP 用户组成员");
        requireCluster(cluster, "请选择所属集群后再更新 LDAP 用户组成员");
        List<String> memberUids = new ArrayList<>();
        if (body != null && body.get("members") instanceof List) {
            for (Object item : (List<?>) body.get("members")) {
                if (item != null) {
                    memberUids.add(String.valueOf(item));
                }
            }
        }
        return ldapService.updatePosixGroupMembers(cluster, groupName, memberUids);
    }

    @DeleteMapping("/ldap-groups/{groupName}")
    @Operation(summary = "删除 LDAP 用户组", description = "删除 posixGroup。若组仍为用户主组则拒绝删除。")
    public Map<String, Object> deleteLdapGroup(@PathVariable String groupName,
                                               @RequestParam(required = false) String cluster,
                                               @RequestParam(defaultValue = "false") boolean force,
                                               HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可删除 LDAP 用户组");
        requireCluster(cluster, "请选择所属集群后再删除 LDAP 用户组");
        return ldapService.deletePosixGroup(cluster, groupName, force);
    }

    @GetMapping("/user/{username}/ldap-group")
    @Operation(summary = "查询用户所属 LDAP 组", description = "返回用户当前主组信息，用于 OpenLDAP 用户管理。")
    public Map<String, Object> getUserLdapGroup(@PathVariable String username,
                                                @RequestParam(required = false) String cluster,
                                                HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询用户所属 LDAP 组");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再查询用户所属 LDAP 组");
        }
        return ldapService.getUserPrimaryGroup(cluster, username);
    }

    @PutMapping("/user/{username}/ldap-group")
    @Operation(summary = "修改用户所属 LDAP 组", description = "更新 OpenLDAP 用户主组，并同步组 memberUid。")
    public Map<String, Object> updateUserLdapGroup(@PathVariable String username,
                                                   @RequestParam(required = false) String cluster,
                                                   @RequestBody Map<String, Object> body,
                                                   HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可修改用户所属 LDAP 组");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再修改用户所属 LDAP 组");
        }
        String groupName = body == null ? null : (body.get("groupName") == null ? null : String.valueOf(body.get("groupName")));
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择新的 LDAP 用户组");
        }
        Map<String, Object> result = ldapService.updateUserPrimaryGroup(cluster, username, groupName.trim());
        refreshStoredLdapData(username, resolveClusterName(cluster), cluster);
        return result;
    }

    @GetMapping("/user/{username}/ldap-profile")
    @Operation(summary = "查询用户完整 LDAP 属性", description = "返回 LDAP 原始属性、主组、附加组和锁定状态。")
    public Map<String, Object> getUserLdapProfile(@PathVariable String username,
                                                  @RequestParam(required = false) String cluster,
                                                  HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询用户 LDAP 属性");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再查询用户 LDAP 属性");
        }
        return ldapService.getUserLdapProfile(cluster, username);
    }

    @PutMapping("/user/{username}/ldap-password")
    @Operation(summary = "重置 LDAP 密码", description = "重置 OpenLDAP 用户密码，并同步更新 DGA 本地加密密码。")
    public Map<String, Object> resetUserLdapPassword(@PathVariable String username,
                                                     @RequestParam(required = false) String cluster,
                                                     @RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可重置 LDAP 密码");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再重置 LDAP 密码");
        }
        String password = body == null ? null : (body.get("password") == null ? null : String.valueOf(body.get("password")));
        if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入新的 LDAP 密码");
        }
        Map<String, Object> result = ldapService.resetUserPassword(cluster, username, password);
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, resolveClusterName(cluster));
        if (user != null) {
            user.setPassword(passwordEncoder.encode(password));
            dgaUserRepository.save(user);
        }
        return result;
    }

    @PutMapping("/user/{username}/ldap-lock")
    @Operation(summary = "锁定或解锁 LDAP 用户", description = "通过 OpenLDAP 条目属性控制用户锁定状态。")
    public Map<String, Object> updateUserLdapLock(@PathVariable String username,
                                                  @RequestParam(required = false) String cluster,
                                                  @RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可锁定或解锁 LDAP 用户");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再更新锁定状态");
        }
        Object lockedValue = body == null ? null : body.get("locked");
        if (!(lockedValue instanceof Boolean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locked 字段必须为 true 或 false");
        }
        Map<String, Object> result = ldapService.updateUserLockStatus(cluster, username, (Boolean) lockedValue);
        refreshStoredLdapData(username, resolveClusterName(cluster), cluster);
        return result;
    }

    @GetMapping("/user/{username}/ldap-supplementary-groups")
    @Operation(summary = "查询用户附加 LDAP 组", description = "返回用户当前附加组列表，不含主组。")
    public List<Map<String, Object>> getUserSupplementaryGroups(@PathVariable String username,
                                                                @RequestParam(required = false) String cluster,
                                                                HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可查询用户附加组");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再查询用户附加组");
        }
        return ldapService.getUserSupplementaryGroups(cluster, username);
    }

    @PutMapping("/user/{username}/ldap-supplementary-groups")
    @Operation(summary = "更新用户附加 LDAP 组", description = "批量更新用户附加组 memberUid，不修改主组。")
    public Map<String, Object> updateUserSupplementaryGroups(@PathVariable String username,
                                                             @RequestParam(required = false) String cluster,
                                                             @RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可更新用户附加组");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择所属集群后再更新用户附加组");
        }
        List<String> groupNames = new ArrayList<>();
        if (body != null && body.get("groupNames") instanceof List) {
            for (Object item : (List<?>) body.get("groupNames")) {
                if (item != null) {
                    groupNames.add(String.valueOf(item));
                }
            }
        }
        Map<String, Object> result = ldapService.updateUserSupplementaryGroups(cluster, username, groupNames);
        refreshStoredLdapData(username, resolveClusterName(cluster), cluster);
        return result;
    }

    @PostMapping("/user/{username}/repair-ldap")
    @Operation(summary = "修复 OpenLDAP 用户系统属性", description = "为 OpenLDAP 创建的用户补齐 POSIX 账号属性，使其可被操作系统识别。")
    public Map<String, Object> repairLdapUser(@PathVariable String username,
                                              @RequestParam(required = false) String cluster,
                                              HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可修复 OpenLDAP 用户");
        String clusterName = resolveClusterName(cluster);
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "用户 " + username + " 不存在于集群 " + clusterName);
        }
        String strategy = user.getCreationStrategy() == null ? "" : user.getCreationStrategy().toUpperCase();
        if (!isLdapManagedStrategy(strategy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "仅支持修复 OpenLDAP/LDAP 用户，当前来源为: " + user.getCreationStrategy());
        }
        Map<String, Object> result = ldapService.repairPosixAccount(cluster, username);
        refreshStoredLdapData(username, clusterName, cluster);
        return result;
    }
    
    @PostMapping("/import")
    @Operation(summary = "按集群导入 LDAP 用户", description = "从指定集群的 OpenLDAP 端点拉取用户，并执行新增、更新和历史修复。")
    public Map<String, Object> importUsers(@RequestParam(required = false) String cluster,
                                           HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可导入 OpenLDAP 用户");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择具体集群后再导入 OpenLDAP 用户");
        }

        com.dga.cluster.entity.Cluster targetCluster = resolveCluster(cluster);
        String clusterName = targetCluster != null && targetCluster.getClusterName() != null
                ? targetCluster.getClusterName()
                : resolveClusterName(cluster);
        dgaUserSchemaService.ensureClusterScopedUsernameConstraint();
        try {
            List<Map<String, Object>> ldapUsers = ldapService.listUsers(cluster);
            Map<String, Object> searchInfo = ldapService.describeUserSearch(cluster);
            int inserted = 0;
            int updated = 0;
            int skipped = 0;
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
                    Map<String, Object> before = isInsert ? null : importComparableSnapshot(user);
                    user.setFirstName(firstNonBlank(ldapValue(u, "givenName"), ldapValue(u, "cn"), username));
                    user.setLastName(firstNonBlank(ldapValue(u, "sn"), username));
                    user.setEmail(ldapValue(u, "mail"));
                    user.setCreationStrategy("LDAP_IMPORT");
                    user.setClusterName(clusterName);
                    user.setDeleted(false);
                    syncDgaUserFromLdap(user, cluster, username);
                    Map<String, Object> after = importComparableSnapshot(user);
                    if (isInsert) {
                        dgaUserRepository.save(user);
                        inserted++;
                    } else if (before.equals(after)) {
                        skipped++;
                    } else {
                        dgaUserRepository.save(user);
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
            result.put("skipped", skipped);
            result.put("repaired", repaired);
            result.put("failed", failed);
            result.put("failures", failures);
            result.put("search", searchInfo);
            result.put("message", buildImportMessage(ldapUsers.size(), inserted, updated, skipped, repaired, failed, searchInfo));
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage() == null ? "OpenLDAP 导入失败" : e.getMessage(), e);
        }
    }

    private String buildImportMessage(int total, int inserted, int updated, int skipped, int repaired, int failed, Map<String, Object> searchInfo) {
        if (total == 0) {
            Object searchBaseDn = searchInfo == null ? null : searchInfo.get("searchBaseDn");
            Object filter = searchInfo == null ? "(uid=*)" : searchInfo.getOrDefault("filter", "(uid=*)");
            return "LDAP 查询成功，但没有找到 uid 用户。请检查 User Base DN 是否为用户所在目录，当前搜索范围: "
                    + (searchBaseDn == null ? "未配置" : searchBaseDn)
                    + "，过滤条件: " + filter;
        }
        String repairedText = repaired > 0 ? "，历史修复 " + repaired : "";
        return "导入完成：新增 " + inserted + "，更新 " + updated + "，跳过 " + skipped + repairedText + "，失败 " + failed;
    }

    @PostMapping("/import-auth-backend")
    @Operation(summary = "导入 SQL 授权后端用户", description = "从 StarRocks 或 Doris 授权后端拉取用户，并尽量匹配当前用户表执行新增或更新。")
    public Map<String, Object> importAuthBackendUsers(@RequestParam(required = false) String cluster,
                                                      HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可导入授权后端用户");
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择具体 StarRocks 或 Doris 集群后再导入用户");
        }
        com.dga.cluster.entity.Cluster targetCluster = resolveCluster(cluster);
        String clusterName = targetCluster != null && targetCluster.getClusterName() != null
                ? targetCluster.getClusterName()
                : resolveClusterName(cluster);
        String sqlEngine = resolveSqlAuthEngine(cluster, targetCluster);
        if (sqlEngine == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前集群不是 StarRocks 或 Doris，请使用 OpenLDAP 导入");
        }
        dgaUserSchemaService.ensureClusterScopedUsernameConstraint();
        try {
            List<String> principals = authorizationService.listPrincipals(cluster);
            int inserted = 0;
            int updated = 0;
            int skipped = 0;
            int repaired = 0;
            int failed = 0;
            List<Map<String, Object>> failures = new ArrayList<>();
            String strategy = sqlEngine;
            for (String principal : principals) {
                try {
                    String username = principal == null ? null : principal.trim();
                    if (username == null || username.isEmpty()) {
                        throw new IllegalArgumentException("授权后端用户为空");
                    }
                    DgaUser user = findImportTargetUser(username, targetCluster, clusterName);
                    boolean isInsert = user == null;
                    boolean isLegacyRepair = false;
                    if (isInsert) {
                        user = new DgaUser();
                        user.setUsername(username);
                    } else if (!clusterName.equals(user.getClusterName())) {
                        isLegacyRepair = true;
                    }
                    Map<String, Object> before = isInsert ? null : importComparableSnapshot(user);
                    user.setFirstName(firstNonBlank(user.getFirstName(), username));
                    user.setLastName(firstNonBlank(user.getLastName(), username));
                    user.setDisplayName(firstNonBlank(user.getDisplayName(), username));
                    user.setCreationStrategy(strategy);
                    user.setClusterName(clusterName);
                    user.setDeleted(false);
                    Map<String, Object> after = importComparableSnapshot(user);
                    if (isInsert) {
                        dgaUserRepository.save(user);
                        inserted++;
                    } else if (before.equals(after)) {
                        skipped++;
                    } else {
                        dgaUserRepository.save(user);
                        updated++;
                        if (isLegacyRepair) {
                            repaired++;
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    Map<String, Object> failure = new HashMap<>();
                    failure.put("entry", principal);
                    failure.put("message", e.getMessage());
                    failures.add(failure);
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("cluster", clusterName);
            result.put("engine", strategy);
            result.put("total", principals.size());
            result.put("inserted", inserted);
            result.put("updated", updated);
            result.put("skipped", skipped);
            result.put("repaired", repaired);
            result.put("failed", failed);
            result.put("failures", failures);
            result.put("message", buildAuthBackendImportMessage(strategy, principals.size(), inserted, updated, skipped, repaired, failed));
            return result;
        } catch (Exception e) {
            String message = e.getMessage() == null ? "授权后端用户导入失败" : e.getMessage();
            if (message.contains("Access denied") && message.contains("GRANT")) {
                message = "StarRocks 端点账号缺少 SYSTEM 上的 GRANT 权限，无法执行 SHOW USERS 查询授权用户";
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, e);
        }
    }

    private String buildAuthBackendImportMessage(String engine, int total, int inserted, int updated, int skipped, int repaired, int failed) {
        String repairedText = repaired > 0 ? "，历史修复 " + repaired : "";
        return engine + " 用户导入完成：发现 " + total + "，新增 " + inserted + "，更新 " + updated + "，跳过 " + skipped + repairedText + "，失败 " + failed;
    }

    private void validateSqlAuthUsername(String username, String clusterType) {
        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        String text = username.trim();
        String user = text;
        String host = "%";
        int atIndex = text.indexOf('@');
        if (atIndex > 0) {
            user = text.substring(0, atIndex).trim();
            host = text.substring(atIndex + 1).trim();
        }
        if (!user.matches("^[A-Za-z][A-Za-z0-9_]{1,63}$")) {
            String engine = clusterType != null && clusterType.toUpperCase().contains("DORIS") ? "Doris" : "StarRocks";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    engine + " 用户名必须以字母开头，只能包含字母、数字、下划线，长度 2-64；如需指定 host 可使用 user@host");
        }
        if (!"%".equals(host) && !host.matches("^[A-Za-z0-9_.%-]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户 host 只能包含字母、数字、下划线、点、百分号和横线");
        }
    }

    @PostMapping("/sync/{username}")
    @Operation(summary = "同步用户权限", description = "从集群授权后端重新拉取指定用户的当前权限，并同步到本地授权记录。")
    public String syncUserPermissions(@PathVariable String username, @RequestParam(required = false) String cluster) {
        String targetCluster = (cluster != null && !cluster.isEmpty()) ? cluster : "CDH-Cluster-01";

        String sqlEngine = resolveSqlAuthEngine(targetCluster, resolveCluster(targetCluster));
        List<Map<String, Object>> hivePermsRaw = sqlEngine != null
                ? authorizationService.getUserPermissions(username, targetCluster)
                : hiveAuthService.getUserPermissions(username, targetCluster);
        Set<String> hivePermKeys = new HashSet<>();
        List<UserHiveAccess> toSave = new ArrayList<>();

        for (Map<String, Object> row : hivePermsRaw) {
            Map<String, Object> lowerRow = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            lowerRow.putAll(row);

            String db = (String) lowerRow.get("database");
            if (db == null) db = (String) lowerRow.get("database_name");

            String table = (String) lowerRow.get("table");
            if (table == null) table = (String) lowerRow.get("table_name");
            table = normalizeAuthTable(table);

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

        for (String key : hivePermKeys) {
            String[] parts = key.split("\\|", -1);
            String db = parts[0];
            String table = parts[1].isEmpty() ? null : parts[1];
            String perm = parts[2];

            boolean found = false;
            for (UserHiveAccess local : localPerms) {
                if (local.getDatabaseName().equals(db)
                        && Objects.equals(local.getTableName(), table)
                        && local.getPermission().equals(perm)) {
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
    @Operation(summary = "分页查询权限用户", description = "按集群和关键词分页查询权限管理页展示的用户列表。")
    public Page<DgaUser> listUsers(@RequestParam(defaultValue = "0") int page, 
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String cluster,
                                   @RequestParam(value = "q", required = false) String query) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        List<String> excludedStrategies = java.util.Arrays.asList("SELF_REGISTER", "SELF_REG");
        
        Page<DgaUser> storedUsers;
        if (cluster != null && !cluster.isEmpty()) {
            String clusterName = resolveClusterName(cluster);
            if (query != null && !query.isEmpty()) {
                storedUsers = dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotInAndUsernameContainingIgnoreCase(
                        clusterName, excludedStrategies, query, pageable);
            } else {
                storedUsers = dgaUserRepository.findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(
                        clusterName, excludedStrategies, pageable);
            }
            if (!storedUsers.isEmpty()) {
                return storedUsers;
            }
            return livePrincipalUsers(cluster, query, pageable);
        }
        if (query != null && !query.isEmpty()) {
            return dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotInAndUsernameContainingIgnoreCase(
                    excludedStrategies, query, pageable);
        }
        return dgaUserRepository.findByIsDeletedFalseAndCreationStrategyNotIn(
                excludedStrategies, pageable);
    }

    private Page<DgaUser> livePrincipalUsers(String cluster, String query, Pageable pageable) {
        List<String> principals = authorizationService.listPrincipals(cluster);
        String keyword = query == null ? null : query.trim().toLowerCase();
        List<DgaUser> users = new ArrayList<>();
        com.dga.cluster.entity.Cluster clusterObject = resolveCluster(cluster);
        String clusterName = clusterObject != null && clusterObject.getClusterName() != null ? clusterObject.getClusterName() : cluster;
        String type = clusterObject != null && clusterObject.getType() != null ? clusterObject.getType().toUpperCase() : "";
        String sqlEngine = resolveSqlAuthEngine(cluster, clusterObject);
        String strategy = sqlEngine != null ? sqlEngine : "LIVE_AUTH_BACKEND";
        for (String principal : principals) {
            if (principal == null || principal.trim().isEmpty()) {
                continue;
            }
            String username = principal.trim();
            if (keyword != null && !keyword.isEmpty() && !username.toLowerCase().contains(keyword)) {
                continue;
            }
            DgaUser user = new DgaUser();
            user.setUsername(username);
            user.setClusterName(clusterName);
            user.setCreationStrategy(strategy);
            user.setDeleted(false);
            users.add(user);
        }
        int start = Math.min((int) pageable.getOffset(), users.size());
        int end = Math.min(start + pageable.getPageSize(), users.size());
        return new org.springframework.data.domain.PageImpl<>(users.subList(start, end), pageable, users.size());
    }

    @PutMapping("/user/{username}/protection")
    @Operation(summary = "设置保护用户", description = "仅 root admin 可将指定权限用户设置或取消为保护用户。")
    public DgaUser updateUserProtection(@PathVariable String username,
                                        @RequestParam(required = false) String cluster,
                                        @RequestParam("protected") boolean protectedUser,
                                        HttpServletRequest request) {
        adminGuard.requireRootAdmin(request);
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "设置保护用户必须指定所属集群");
        }
        String clusterName = resolveClusterName(cluster);
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在: " + username + " (" + clusterName + ")");
        }
        user.setProtectedUser(protectedUser);
        return dgaUserRepository.save(user);
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
    public List<String> listResourceDatabases(@RequestParam(required = false) String cluster,
                                              @RequestParam(required = false) String authBackend) {
        return authorizationService.listDatabases(cluster, authBackend);
    }

    @GetMapping("/capabilities")
    @Operation(summary = "查询集群授权能力", description = "返回指定集群当前的授权引擎、依赖端点与可用能力。")
    public AuthorizationCapability authorizationCapability(@RequestParam(required = false) String cluster,
                                                           @RequestParam(required = false) String authBackend) {
        return authorizationService.capability(cluster, authBackend);
    }

    @GetMapping("/capabilities/backends")
    @Operation(summary = "查询集群授权后端候选", description = "返回指定集群当前可识别的授权后端能力列表。")
    public List<AuthorizationCapability> authorizationBackends(@RequestParam(required = false) String cluster) {
        return authorizationService.backendCapabilities(cluster);
    }

    @GetMapping("/hive/tables")
    public List<String> listTables(@RequestParam("database") String database, @RequestParam(required = false) String cluster) {
        return hiveAuthService.listTables(cluster, database);
    }

    @GetMapping("/resources/tables")
    public List<String> listResourceTables(@RequestParam("database") String database,
                                           @RequestParam(required = false) String cluster,
                                           @RequestParam(required = false) String authBackend) {
        return authorizationService.listTables(cluster, database, authBackend);
    }

    @GetMapping("/resources/principals")
    public List<Map<String, Object>> listResourcePrincipals(@RequestParam(required = false) String cluster,
                                                            @RequestParam(required = false) String authBackend,
                                                            @RequestParam(required = false) String subjectType,
                                                            @RequestParam(required = false) String groupName) {
        String normalizedSubjectType = firstNonBlank(subjectType, "USER").toUpperCase(Locale.ROOT);
        if ("GROUP".equals(normalizedSubjectType)) {
            return listGroupPrincipalOptions(cluster, authBackend);
        }
        if (!"USER".equals(normalizedSubjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectType 仅支持 USER 或 GROUP");
        }
        return listUserPrincipalOptions(cluster, authBackend, groupName);
    }

    private List<Map<String, Object>> listUserPrincipalOptions(String cluster, String authBackend, String groupName) {
        TreeMap<String, Map<String, Object>> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String normalizedGroupName = firstNonBlank(groupName);
        Set<String> groupUsers = normalizedGroupName == null ? null : groupUsernames(cluster, normalizedGroupName);
        boolean backendRequiresExistingUser = authorizationService.requiresExistingBackendUser(cluster, authBackend);
        List<String> principals;
        try {
            principals = authorizationService.listPrincipals(cluster, authBackend);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readablePrincipalListError(e), e);
        }
        boolean rangerBackend = isRangerAuthBackend(authBackend);
        java.util.Set<String> backendUsers = new java.util.LinkedHashSet<>();
        if (principals != null) {
            for (String principal : principals) {
                String username = firstNonBlank(principal);
                if (username != null) {
                    backendUsers.add(lower(username));
                    if (groupUsers != null && !groupUsers.contains(lower(username))) {
                        continue;
                    }
                    options.putIfAbsent(username, principalOption(username, "USER", rangerBackend ? "RANGER_USER" : "AUTH_BACKEND",
                            true, true, true, true, false, new ArrayList<>()));
                }
            }
        }
        if (rangerBackend) {
            return new ArrayList<>(options.values());
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
        for (DgaUser user : users.getContent()) {
            String username = firstNonBlank(user.getUsername());
            if (username == null) {
                continue;
            }
            if (groupUsers != null && !groupUsers.contains(lower(username)) && !userBelongsToGroupSnapshot(user, normalizedGroupName)) {
                continue;
            }
            boolean existsInBackend = backendUsers.contains(lower(username));
            boolean historical = hasRecordedAccess(username, cluster) || existsInBackend;
            boolean assignable = !backendRequiresExistingUser || existsInBackend;
            boolean directExceptionAllowed = historical && (!backendRequiresExistingUser || existsInBackend);
            boolean requiresRoleBinding = !directExceptionAllowed && assignable;
            List<String> warnings = new ArrayList<>();
            if (backendRequiresExistingUser && !existsInBackend) {
                warnings.add(firstNonBlank(authBackend, "授权后端") + " 未发现该用户，请先创建/导入后再绑定角色或执行直接例外授权。");
            } else if (!historical) {
                warnings.add("该对象会被视为新用户，需先完成角色绑定后再执行直接例外授权/回收。");
            }
            options.putIfAbsent(username, principalOption(username, "USER", "DGA_USER",
                    !backendRequiresExistingUser || existsInBackend,
                    historical,
                    assignable,
                    directExceptionAllowed,
                    requiresRoleBinding,
                    warnings));
        }
        return new ArrayList<>(options.values());
    }

    private Set<String> groupUsernames(String cluster, String groupName) {
        Set<String> usernames = new LinkedHashSet<>();
        try {
            Map<String, Object> group = ldapService.getPosixGroup(cluster, groupName);
            addUsernames(usernames, group.get("boundUsers"));
            addUsernames(usernames, group.get("members"));
            addUsernames(usernames, group.get("primaryUsers"));
        } catch (Exception e) {
            System.err.println("LDAP group member lookup failed for " + groupName + ": " + e.getMessage());
        }

        String resolvedCluster = cluster;
        if (cluster != null && !cluster.trim().isEmpty()) {
            com.dga.cluster.entity.Cluster found = clusterRepository.findByClusterCode(cluster);
            if (found != null && found.getClusterName() != null && !found.getClusterName().trim().isEmpty()) {
                resolvedCluster = found.getClusterName();
            }
        }
        for (DgaUser user : dgaUserRepository.findActiveUsersByCluster(resolvedCluster)) {
            if (userBelongsToGroupSnapshot(user, groupName)) {
                String username = firstNonBlank(user.getUsername());
                if (username != null) {
                    usernames.add(lower(username));
                }
            }
        }
        return usernames;
    }

    private void addUsernames(Set<String> usernames, Object value) {
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                addUsernames(usernames, item);
            }
            return;
        }
        String username = value == null ? null : firstNonBlank(String.valueOf(value));
        if (username != null) {
            usernames.add(lower(username));
        }
    }

    private boolean userBelongsToGroupSnapshot(DgaUser user, String groupName) {
        if (user == null || groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        if (sameText(user.getPrimaryGroupName(), groupName)) {
            return true;
        }
        String supplementary = user.getSupplementaryGroups();
        if (supplementary == null) {
            return false;
        }
        for (String group : supplementary.split(",")) {
            if (sameText(group, groupName)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> listGroupPrincipalOptions(String cluster, String authBackend) {
        if (isRangerAuthBackend(authBackend)) {
            return authorizationService.listGroups(cluster, authBackend).stream()
                    .map(groupName -> principalOption(groupName, "GROUP", "RANGER_GROUP",
                            true, true, true, false, false, new ArrayList<>()))
                    .collect(Collectors.toList());
        }
        return listGroupPrincipals(cluster).stream()
                .map(groupName -> principalOption(groupName, "GROUP", "LDAP_GROUP",
                        true, true, true, false, false, new ArrayList<>()))
                .collect(Collectors.toList());
    }

    private boolean isRangerAuthBackend(String authBackend) {
        return authBackend != null && authBackend.trim().toUpperCase(Locale.ROOT).contains("RANGER");
    }

    private Map<String, Object> principalOption(String name, String subjectType, String source,
                                                boolean exists, boolean historical, boolean assignable,
                                                boolean directExceptionAllowed, boolean requiresRoleBinding,
                                                List<String> warnings) {
        Map<String, Object> option = new HashMap<>();
        option.put("name", name);
        option.put("value", name);
        option.put("label", name);
        option.put("subjectType", subjectType);
        option.put("source", source);
        option.put("exists", exists);
        option.put("historical", historical);
        option.put("assignable", assignable);
        option.put("directExceptionAllowed", directExceptionAllowed);
        option.put("requiresRoleBinding", requiresRoleBinding);
        option.put("warnings", warnings == null ? new ArrayList<>() : warnings);
        return option;
    }

    private String readablePrincipalListError(Exception e) {
        String message = e.getMessage() == null ? "加载授权用户失败" : e.getMessage();
        if (message.contains("Access denied") && message.contains("GRANT")) {
            return "StarRocks 端点账号缺少 SYSTEM 上的 GRANT 权限，无法执行 SHOW USERS 查询授权用户";
        }
        return message;
    }

    private List<String> listGroupPrincipals(String cluster) {
        try {
            return ldapService.listPosixGroups(cluster).stream()
                    .map(group -> group.get("name"))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .sorted(String::compareToIgnoreCase)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @GetMapping("/users/{username}/audit-timeline")
    @Operation(summary = "查询用户审计时间线", description = "聚合用户授权、回收、角色绑定等操作记录。")
    public List<Map<String, Object>> userAuditTimeline(@PathVariable String username,
                                                       @RequestParam(required = false) String cluster) {
        String resolvedCluster = authorizationService.resolveClusterCodeOrName(cluster);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserResourceAccess access : userResourceAccessRepository.findAuditByUsernameAndCluster(username, resolvedCluster)) {
            rows.add(permissionAuditRow(access));
        }
        for (AuthRoleAssignmentAudit audit : authRoleAssignmentAuditRepository.findUserTimelineAudits(username, resolvedCluster)) {
            rows.add(roleAssignmentAuditRow(audit));
        }
        rows.sort((left, right) -> String.valueOf(right.get("timeValue")).compareTo(String.valueOf(left.get("timeValue"))));
        return rows.stream().limit(80).collect(Collectors.toList());
    }

    @GetMapping("/resources/permissions")
    @Operation(summary = "查询用户实时权限", description = "查询指定集群授权后端中的实时权限明细，包含库、表和权限类型。")
    public Map<String, Object> listResourcePermissions(@RequestParam("username") String username,
                                                       @Parameter(description = "集群编码或集群名称") @RequestParam(required = false) String cluster,
                                                       @RequestParam(required = false) String authBackend) {
        try {
            List<Map<String, Object>> rawPermissions = authorizationService.getUserPermissions(username, cluster, authBackend);
            String resolvedCluster = authorizationService.resolveClusterCodeOrName(cluster);
            List<UserResourceAccess> recordedAccess = resolvedCluster == null || resolvedCluster.trim().isEmpty()
                    ? userResourceAccessRepository.findByUsernameAndIsDeletedFalse(username)
                    : userResourceAccessRepository.findByUsernameAndClusterCodeAndIsDeletedFalse(username, resolvedCluster);
            Map<String, Object> res = new HashMap<>();
            res.put("username", username);
            res.put("cluster", resolvedCluster);
            res.put("engineType", authorizationService.engineType(cluster, authBackend));
            res.put("authBackend", authorizationService.authBackend(cluster, authBackend));
            res.put("source", "LIVE_AUTH_BACKEND");
            res.put("grants", normalizePermissionRows(rawPermissions));
            res.put("recordedGrants", normalizeRecordedPermissionRows(recordedAccess));
            res.put("raw", rawPermissions);
            return res;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }

    @PostMapping("/starrocks/inventory/permissions")
    @Operation(summary = "盘点 StarRocks 用户当前权限", description = "批量拉取 StarRocks 用户当前 live 权限，并按权限组合聚类输出模板候选。")
    public Map<String, Object> inventoryStarRocksPermissions(@RequestBody StarRocksPermissionInventoryRequest request,
                                                             HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行 StarRocks 权限盘点");
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "盘点参数不能为空");
        }
        String cluster = requireText(request.getCluster(), "请选择 StarRocks 集群");
        String authBackend = authorizationService.normalizeAuthBackend(
                firstNonBlank(request.getAuthBackend(), authorizationService.authBackend(cluster)));
        String engineType = authorizationService.engineType(cluster, authBackend);
        if (!"STARROCKS".equalsIgnoreCase(engineType) && !"STARROCKS_SQL".equalsIgnoreCase(authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前集群不是 StarRocks 授权后端，无法执行 StarRocks 权限盘点");
        }

        String resolvedCluster = authorizationService.resolveClusterCodeOrName(cluster);
        boolean includeRecordedGrants = request.getIncludeRecordedGrants() == null || request.getIncludeRecordedGrants();
        List<String> usernames = resolveStarRocksInventoryUsernames(request, cluster, resolvedCluster, authBackend);
        if (usernames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到可盘点的 StarRocks 用户");
        }

        List<Map<String, Object>> users = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        for (String username : usernames) {
            try {
                users.add(buildStarRocksInventoryUser(username, cluster, resolvedCluster, authBackend, includeRecordedGrants));
            } catch (Exception e) {
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("username", username);
                failure.put("message", readableAuthorizationError(e));
                failures.add(failure);
            }
        }
        users.sort(Comparator.comparing(item -> String.valueOf(item.get("username")), String.CASE_INSENSITIVE_ORDER));
        List<Map<String, Object>> templateCandidates = buildStarRocksTemplateCandidates(users);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cluster", resolvedCluster);
        result.put("engineType", engineType);
        result.put("authBackend", authBackend);
        result.put("scannedAt", LocalDateTime.now().toString());
        result.put("requestedUserCount", usernames.size());
        result.put("scannedUserCount", users.size());
        result.put("successCount", users.size());
        result.put("failedCount", failures.size());
        result.put("templateCandidateCount", templateCandidates.size());
        result.put("status", failures.isEmpty() ? "SUCCESS" : (users.isEmpty() ? "FAILED" : "PARTIAL_SUCCESS"));
        result.put("message", "StarRocks 权限盘点完成：扫描 " + usernames.size()
                + "，成功 " + users.size()
                + "，失败 " + failures.size()
                + "，模板候选 " + templateCandidates.size());
        result.put("users", users);
        result.put("templateCandidates", templateCandidates);
        result.put("failures", failures);
        return result;
    }

    @PostMapping("/grant/batch")
    @Operation(summary = "批量授予 Hive 权限", description = "批量授予数据库或表级 Hive 权限，并写入本地授权记录。")
    public String batchGrant(@RequestBody BatchGrantRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行授权操作");
        String username = request.getUsername();
        List<String> permissions = resolvePermissionsOrThrow(request);
        List<String> databases = request.getDatabases();
        String operator = currentOperator();
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
                for (String permission : permissions) {
                    try {
                        hiveAuthService.grantPermission(username, database, permission, clusterName);

                        UserHiveAccess access = new UserHiveAccess();
                        access.setUsername(username);
                        access.setClusterName(clusterName);
                        access.setDatabaseName(database);
                        access.setPermission(permission);
                        access.setGrantedBy(operator);
                        access.setGrantTime(LocalDateTime.now());
                        access.setStatus("ACTIVE");
                        userHiveAccessRepository.save(access);
                        saveResourceAccess(username, clusterName, database, null, permission, operator, "DGA_GRANT");
                        System.out.println("Saved DB access: " + database + " / " + permission);
                    } catch (Exception e) {
                        System.err.println("Failed to grant/save DB access: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }
        List<TableGrant> tables = request.getTables();
        if (tables != null) {
            for (TableGrant tableGrant : tables) {
                for (String permission : permissions) {
                    try {
                        hiveAuthService.grantTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission, clusterName);

                        UserHiveAccess access = new UserHiveAccess();
                        access.setUsername(username);
                        access.setClusterName(clusterName);
                        access.setDatabaseName(tableGrant.getDatabase());
                        access.setTableName(tableGrant.getTable());
                        access.setPermission(permission);
                        access.setGrantedBy(operator);
                        access.setGrantTime(LocalDateTime.now());
                        access.setStatus("ACTIVE");
                        userHiveAccessRepository.save(access);
                        saveResourceAccess(username, clusterName, tableGrant.getDatabase(), tableGrant.getTable(),
                                permission, operator, "DGA_GRANT");
                        System.out.println("Saved Table access: " + tableGrant.getTable() + " / " + permission);
                    } catch (Exception e) {
                        System.err.println("Failed to grant/save Table access: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }
        return "Batch access granted for user: " + username;
    }

    @PostMapping("/grants/batch")
    public String batchGrantResource(@RequestBody BatchGrantRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行授权操作");
        String username = request.getUsername();
        String operator = currentOperator();
        String cluster = request.getCluster() != null && !request.getCluster().isEmpty()
                ? request.getCluster() : "CDH-Cluster-01";
        String authBackend = authorizationService.normalizeAuthBackend(request.getAuthBackend());
        String grantMode = request.getGrantMode() == null || request.getGrantMode().trim().isEmpty()
                ? "ROLE" : request.getGrantMode().trim().toUpperCase();
        String subjectType = firstNonBlank(request.getSubjectType(), "USER").toUpperCase();
        String subjectName = firstNonBlank(request.getSubjectName(), username);

        try {
            if (!"DIRECT_EXCEPTION".equals(grantMode)) {
                if (isRoleSubsetRequest(request)) {
                    return grantRolePermissionSubset(request, username, operator, cluster, authBackend, subjectType, subjectName);
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "RBAC 角色模式必须提交角色范围内勾选的 rolePermissions；授权工作台不能直接扩展角色范围或授予完整角色");
            }
            List<String> permissions = resolvePermissionsOrThrow(request);
            validateDirectException(request, cluster, authBackend, username);
            LocalDateTime expiresAt = parseOptionalDateTime(request.getExpiresAt());
            if (request.getDatabases() != null) {
                for (String database : request.getDatabases()) {
                    for (String permission : permissions) {
                        GrantCommand command = buildGrantCommand(username, cluster, database, null, permission);
                        authorizationService.grant(command, authBackend);
                        saveResourceAccess(username, cluster, database, null, permission, operator,
                                "DIRECT_EXCEPTION", "ACTIVE", false, null, "DIRECT_EXCEPTION", null,
                                "USER", username, request.getExceptionReason(), request.getTicketNo(), request.getApprover(),
                                expiresAt, firstNonBlank(request.getRiskLevel(), riskLevelFor(permission)), authBackend);
                    }
                }
            }
            if (request.getTables() != null) {
                for (TableGrant tableGrant : request.getTables()) {
                    for (String permission : permissions) {
                        GrantCommand command = buildGrantCommand(username, cluster, tableGrant.getDatabase(),
                                tableGrant.getTable(), permission);
                        authorizationService.grant(command, authBackend);
                        saveResourceAccess(username, cluster, tableGrant.getDatabase(), tableGrant.getTable(),
                                permission, operator, "DIRECT_EXCEPTION", "ACTIVE", false, null,
                                "DIRECT_EXCEPTION", null, "USER", username, request.getExceptionReason(), request.getTicketNo(),
                                request.getApprover(), expiresAt, firstNonBlank(request.getRiskLevel(), riskLevelFor(permission)), authBackend);
                    }
                }
            }
            return "Temporary direct access granted for user: " + username;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }

    private String roleAssignmentSyncMessage(AuthRoleView view, String subjectType, String subjectName) {
        if (view == null || view.getAssignments() == null) {
            return null;
        }
        String pendingMessage = null;
        for (com.dga.access.entity.AuthUserRole assignment : view.getAssignments()) {
            if (!subjectType.equalsIgnoreCase(assignment.getSubjectType())) {
                continue;
            }
            if (!subjectName.equalsIgnoreCase(assignment.getSubjectName())) {
                continue;
            }
            String status = assignment.getBackendSyncStatus();
            if ("SUCCESS".equalsIgnoreCase(status)) {
                return null;
            }
            if (status != null && pendingMessage == null) {
                String message = assignment.getSyncMessage();
                pendingMessage = status + (message == null || message.trim().isEmpty() ? "" : " - " + message);
            }
        }
        return pendingMessage;
    }

    @GetMapping("/user/access")
    @Operation(summary = "查询本地授权记录", description = "按用户、集群与状态查询 DGA 本地保存的授权记录。")
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
    public String revokeAccess(@RequestBody AccessRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行权限回收操作");
        String operator = currentOperator();
        String cluster = request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01";
        try {
            hiveAuthService.revokePermission(request.getUsername(), request.getDatabase(), request.getPermission(), request.getCluster());
        } catch (Exception e) {
            System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
        }

        revokeDatabaseAccessRecords(request.getUsername(), cluster, request.getDatabase(),
                request.getPermission(), operator, "DGA_REVOKE");

        return "Access revoked successfully for user: " + request.getUsername();
    }

    @PostMapping("/revoke/batch")
    @Operation(summary = "批量回收 Hive 权限", description = "批量回收数据库或表级 Hive 权限，并更新本地授权记录状态。")
    @org.springframework.transaction.annotation.Transactional
    public String batchRevoke(@RequestBody BatchGrantRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行权限回收操作");
        String username = request.getUsername();
        List<String> permissions = resolvePermissionsOrThrow(request);
        String operator = currentOperator();
        String cluster = request.getCluster() != null ? request.getCluster() : "CDH-Cluster-01";

        List<String> databases = request.getDatabases();
        if (databases != null) {
            for (String database : databases) {
                for (String permission : permissions) {
                    try {
                        hiveAuthService.revokePermission(username, database, permission, cluster);
                    } catch (Exception e) {
                        System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
                    }
                    try {
                        revokeDatabaseAccessRecords(username, cluster, database, permission, operator, "DGA_REVOKE");
                    } catch (Exception e) {
                        System.err.println("Failed to soft delete DB access: " + e.getMessage());
                    }
                }
            }
        }
        List<TableGrant> tables = request.getTables();
        if (tables != null) {
            for (TableGrant tableGrant : tables) {
                for (String permission : permissions) {
                    try {
                        hiveAuthService.revokeTablePermission(username, tableGrant.getDatabase(), tableGrant.getTable(), permission, cluster);
                    } catch (Exception e) {
                        System.err.println("Warning: Hive/Ranger revoke failed: " + e.getMessage());
                    }
                    try {
                        revokeTableAccessRecords(username, cluster, tableGrant.getDatabase(),
                                tableGrant.getTable(), permission, operator, "DGA_REVOKE");
                    } catch (Exception e) {
                        System.err.println("Failed to soft delete Table access: " + e.getMessage());
                    }
                }
            }
        }
        return "Batch access revoked for user: " + username;
    }

    @PostMapping("/revokes/batch")
    public String batchRevokeResource(@RequestBody BatchGrantRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可执行权限回收操作");
        String username = request.getUsername();
        String operator = currentOperator();
        String cluster = request.getCluster() != null && !request.getCluster().isEmpty()
                ? request.getCluster() : "CDH-Cluster-01";
        String authBackend = authorizationService.normalizeAuthBackend(request.getAuthBackend());
        String grantMode = request.getGrantMode() == null || request.getGrantMode().trim().isEmpty()
                ? "ROLE" : request.getGrantMode().trim().toUpperCase();
        String subjectType = firstNonBlank(request.getSubjectType(), "USER").toUpperCase();
        String subjectName = firstNonBlank(request.getSubjectName(), username);

        try {
            boolean hasResourceSelection = (request.getDatabases() != null && !request.getDatabases().isEmpty())
                    || (request.getTables() != null && !request.getTables().isEmpty());
            if (isForceUserRevoke(request, grantMode)) {
                return forceRevokeUserPermissions(request, requireText(username, "请选择目标用户"), operator, cluster, authBackend);
            }
            if (!"DIRECT_EXCEPTION".equals(grantMode) && isRoleSubsetRequest(request)) {
                return revokeRolePermissionSubset(request, username, operator, cluster, authBackend, subjectType, subjectName);
            }
            if (!"DIRECT_EXCEPTION".equals(grantMode) && !hasResourceSelection) {
                String roleCode = requireText(request.getRoleCode(), "RBAC 角色回收必须选择 roleCode");
                authorizationService.revokeRoleAssignment(cluster, roleCode, subjectType, subjectName, authBackend);
                String syncMessage = roleAssignmentSyncMessage(authRoleService.effectivePermissions(roleCode), subjectType, subjectName);
                return syncMessage == null
                        ? "RBAC role assignment revoked: " + roleCode
                        : "RBAC role assignment revoke submitted: " + syncMessage;
            }

            List<String> permissions = resolvePermissionsOrThrow(request);
            validateDirectException(request, cluster, authBackend, username);
            if (request.getDatabases() != null) {
                for (String database : request.getDatabases()) {
                    for (String permission : permissions) {
                        RevokeCommand command = buildRevokeCommand(username, cluster, database, null, permission);
                        authorizationService.revoke(command, authBackend);
                        revokeDatabaseAccessRecords(username, cluster, database, permission, operator, "DIRECT_EXCEPTION");
                    }
                }
            }
            if (request.getTables() != null) {
                for (TableGrant tableGrant : request.getTables()) {
                    for (String permission : permissions) {
                        RevokeCommand command = buildRevokeCommand(username, cluster, tableGrant.getDatabase(),
                                tableGrant.getTable(), permission);
                        authorizationService.revoke(command, authBackend);
                        revokeTableAccessRecords(username, cluster, tableGrant.getDatabase(),
                                tableGrant.getTable(), permission, operator, "DIRECT_EXCEPTION");
                    }
                }
            }
            return "Temporary direct access revoked for user: " + username;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }

    @DeleteMapping("/user/{username}")
    @Operation(summary = "删除权限用户", description = "按当前授权后端回收权限；LDAP 用户同步删除目录账号，SQL 授权后端用户只软删除平台记录。")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(@PathVariable String username,
                             @RequestParam(required = false) String cluster,
                             HttpServletRequest request) {
        adminGuard.requireDeletePrivilege(request);
        String operator = currentOperator();
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "删除用户必须指定所属集群");
        }
        String clusterName = resolveClusterName(cluster);
        com.dga.cluster.entity.Cluster targetCluster = resolveCluster(cluster);
        String sqlEngine = resolveSqlAuthEngine(cluster, targetCluster);
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (user != null) {
            if (isProtectedBigDataUser(user)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "保护用户禁止删除: " + username);
            }
            boolean sqlAuthUser = sqlEngine != null || isSqlCreationStrategy(user.getCreationStrategy());
            if (sqlAuthUser) {
                try {
                    authorizationService.revokeAll(username, cluster);
                } catch (Throwable e) {
                    System.err.println("Failed to revoke SQL authorization permissions: " + e.getMessage());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "删除用户前回收授权后端权限失败，请处理后重试: " + readableAuthorizationError(new Exception(e)), e);
                }
            } else {
                try {
                    hiveAuthService.revokeAll(username, clusterName);
                } catch (Throwable e) {
                    System.err.println("Failed to revoke Hive permissions: " + e.getMessage());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "删除用户前回收权限失败，请处理后重试: " + readableAuthorizationError(new Exception(e)), e);
                }

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
            }

            user.setDeleted(true);
            dgaUserRepository.save(user);

            try {
                userHiveAccessRepository.softDeleteAllAccessByUsernameAndClusterName(username, clusterName);
                userResourceAccessRepository.softDeleteAllByUsernameAndCluster(username, clusterName, operator);
            } catch (Exception e) {
                System.err.println("Failed to update UserHiveAccess status: " + e.getMessage());
            }

            return "User permissions revoked and soft deleted from DGA system: " + username;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在: " + username + " (" + clusterName + ")");
        }
    }

    private boolean isProtectedBigDataUser(DgaUser user) {
        if (user == null || user.getUsername() == null) {
            return false;
        }
        if (user.getProtectedUser() != null) {
            return Boolean.TRUE.equals(user.getProtectedUser());
        }
        return PROTECTED_BIGDATA_USERS.contains(user.getUsername().trim().toLowerCase());
    }

    private boolean isLdapManagedStrategy(String strategy) {
        String value = strategy == null ? "" : strategy.trim().toUpperCase();
        return "OPENLDAP".equals(value) || "LDAP".equals(value) || "LDAP_IMPORT".equals(value);
    }

    private void requireCluster(String cluster, String message) {
        if (cluster == null || cluster.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String stringBodyValue(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    private Long longBodyValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null || String.valueOf(body.get(key)).trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(body.get(key)).trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 必须是数字", e);
        }
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

    private String resolveSqlAuthEngine(String clusterIdentifier, com.dga.cluster.entity.Cluster targetCluster) {
        try {
            AuthorizationCapability capability = authorizationService.capability(clusterIdentifier);
            if (capability == null || capability.getIdentity() == null) {
                return null;
            }
            if (!"AUTH_BACKEND".equalsIgnoreCase(capability.getIdentity().getMode())) {
                return null;
            }
            return capability.getEngineType();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSqlCreationStrategy(String strategy) {
        if (strategy == null) {
            return false;
        }
        String normalized = strategy.toUpperCase();
        return normalized.contains("STARROCKS") || normalized.contains("DORIS") || normalized.contains("LIVE_AUTH");
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

    private void refreshStoredLdapData(String username, String clusterName, String clusterIdentifier) {
        DgaUser user = dgaUserRepository.findByUsernameAndClusterName(username, clusterName);
        if (user == null) {
            return;
        }
        syncDgaUserFromLdap(user, clusterIdentifier, username);
        dgaUserRepository.save(user);
    }

    private void syncDgaUserFromLdap(DgaUser user, String clusterIdentifier, String username) {
        try {
            Map<String, Object> profile = ldapService.getUserLdapProfile(clusterIdentifier, username);
            syncDgaUserFromLdapSnapshot(user, profile, clusterIdentifier);
        } catch (Exception e) {
            System.err.println("Failed to sync LDAP attributes for " + username + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void syncDgaUserFromLdapSnapshot(DgaUser user, Map<String, Object> snapshot, String clusterIdentifier) {
        if (user == null || snapshot == null) {
            return;
        }
        Map<String, Object> attributes = snapshot.get("attributes") instanceof Map
                ? (Map<String, Object>) snapshot.get("attributes") : snapshot;
        user.setDisplayName(firstNonBlank(ldapValue(attributes, "displayName"), user.getDisplayName(), ldapValue(attributes, "cn")));
        user.setFirstName(firstNonBlank(ldapValue(attributes, "givenName"), user.getFirstName(), ldapValue(attributes, "cn")));
        user.setLastName(firstNonBlank(ldapValue(attributes, "sn"), user.getLastName(), user.getUsername()));
        user.setEmail(firstNonBlank(ldapValue(attributes, "mail"), user.getEmail()));
        user.setLdapDn(firstNonBlank(ldapValue(snapshot, "dn"), user.getLdapDn()));
        user.setUidNumber(parseLongValue(attributes.get("uidNumber")));
        user.setGidNumber(parseLongValue(attributes.get("gidNumber")));
        user.setHomeDirectory(firstNonBlank(ldapValue(attributes, "homeDirectory"), user.getHomeDirectory()));
        user.setLoginShell(firstNonBlank(ldapValue(attributes, "loginShell"), user.getLoginShell()));

        Object primaryGroup = snapshot.get("primaryGroup");
        if (primaryGroup instanceof Map) {
            user.setPrimaryGroupName(firstNonBlank(ldapValue((Map<String, Object>) primaryGroup, "name"), user.getPrimaryGroupName()));
        }
        Object supplementary = snapshot.get("supplementaryGroups");
        if (supplementary instanceof List) {
            List<String> names = new ArrayList<>();
            for (Object item : (List<?>) supplementary) {
                if (item instanceof Map) {
                    String name = ldapValue((Map<String, Object>) item, "name");
                    if (name != null && !name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
            user.setSupplementaryGroups(names.isEmpty() ? null : String.join(",", names));
        }
        Object locked = snapshot.get("locked");
        if (locked instanceof Boolean) {
            user.setLdapLocked((Boolean) locked);
        }
        if (attributes instanceof Map) {
            try {
                user.setLdapAttributesJson(objectMapper.writeValueAsString(attributes));
            } catch (Exception e) {
                System.err.println("Failed to serialize LDAP attributes for " + user.getUsername() + ": " + e.getMessage());
            }
        } else {
            try {
                user.setLdapAttributesJson(objectMapper.writeValueAsString(snapshot));
            } catch (Exception e) {
                System.err.println("Failed to serialize LDAP snapshot for " + user.getUsername() + ": " + e.getMessage());
            }
        }
        if (clusterIdentifier != null && !clusterIdentifier.trim().isEmpty()) {
            user.setClusterName(resolveClusterName(clusterIdentifier));
        }
    }

    private Map<String, Object> importComparableSnapshot(DgaUser user) {
        Map<String, Object> snapshot = new TreeMap<>();
        snapshot.put("username", user.getUsername());
        snapshot.put("firstName", user.getFirstName());
        snapshot.put("lastName", user.getLastName());
        snapshot.put("displayName", user.getDisplayName());
        snapshot.put("email", user.getEmail());
        snapshot.put("creationStrategy", user.getCreationStrategy());
        snapshot.put("clusterName", user.getClusterName());
        snapshot.put("deleted", Boolean.TRUE.equals(user.getDeleted()));
        snapshot.put("ldapDn", user.getLdapDn());
        snapshot.put("uidNumber", user.getUidNumber());
        snapshot.put("gidNumber", user.getGidNumber());
        snapshot.put("homeDirectory", user.getHomeDirectory());
        snapshot.put("loginShell", user.getLoginShell());
        snapshot.put("primaryGroupName", user.getPrimaryGroupName());
        snapshot.put("supplementaryGroups", user.getSupplementaryGroups());
        snapshot.put("ldapLocked", user.getLdapLocked());
        snapshot.put("ldapAttributesJson", user.getLdapAttributesJson());
        return snapshot;
    }

    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isForceUserRevoke(BatchGrantRequest request, String grantMode) {
        return Boolean.TRUE.equals(request.getForceUserRevoke()) || "ADMIN_FORCE_USER".equals(grantMode);
    }

    private boolean isRoleSubsetRequest(BatchGrantRequest request) {
        return Boolean.TRUE.equals(request.getRoleSubsetMode())
                || (request.getRolePermissions() != null && !request.getRolePermissions().isEmpty());
    }

    private String grantRolePermissionSubset(BatchGrantRequest request, String username, String operator,
                                             String cluster, String authBackend, String subjectType, String subjectName) {
        String roleCode = requireText(request.getRoleCode(), "请选择角色");
        String normalizedSubjectType = requireText(subjectType, "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String normalizedSubjectName = requireText(subjectName, "请选择用户或 LDAP 组").trim();
        if (!supportsRoleSubsetGrant(cluster, authBackend, normalizedSubjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "当前授权后端不支持 " + normalizedSubjectType + " 角色权限子集授权，请选择支持的绑定对象类型");
        }
        requireActiveRoleAssignment(roleCode, normalizedSubjectType, normalizedSubjectName, authBackend);
        List<AuthRolePermission> selected = isTableSubsetRequest(request)
                ? validateTableSubsetSelections(roleCode, request.getRolePermissions(), authBackend)
                : validateRolePermissionSelections(roleCode, request.getRolePermissions(), authBackend);
        LocalDateTime expiresAt = parseOptionalDateTime(request.getExpiresAt());
        if ("GROUP".equals(normalizedSubjectType)) {
            if (assignmentUsesMaterializedPolicies(cluster, authBackend, normalizedSubjectType)) {
                for (AuthRolePermission permission : selected) {
                    authorizationService.grantPermissionToGroup(cluster, normalizedSubjectName,
                            permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(), authBackend);
                }
            } else {
                String derivedRoleCode = derivedSubsetRoleCode(roleCode, normalizedSubjectType, normalizedSubjectName, selected);
                authorizationService.ensureRole(cluster, derivedRoleCode, authBackend);
                for (AuthRolePermission permission : selected) {
                    authorizationService.grantPermissionToRole(cluster, derivedRoleCode,
                            permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(), authBackend);
                }
                authorizationService.assignRoleToGroup(cluster, derivedRoleCode, normalizedSubjectName, authBackend);
            }
        } else if ("USER".equals(normalizedSubjectType)) {
            for (AuthRolePermission permission : selected) {
                GrantCommand command = buildGrantCommand(normalizedSubjectName, cluster,
                        permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission());
                authorizationService.grant(command, authBackend);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        for (AuthRolePermission permission : selected) {
            saveResourceAccess("USER".equals(normalizedSubjectType) ? normalizedSubjectName : firstNonBlank(username, normalizedSubjectName),
                    cluster, permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(),
                    operator, "RBAC_ROLE_SUBSET", "ACTIVE", false, null,
                    "ROLE", roleCode, normalizedSubjectType, normalizedSubjectName,
                    null, null, null, expiresAt, riskLevelFor(permission.getPermission()), authBackend);
        }
        if ("GROUP".equals(normalizedSubjectType) && usesNativeGroupRole(cluster, authBackend)) {
            if (userBelongsToLdapGroup(cluster, username, normalizedSubjectName)) {
                return "RBAC 角色权限已下发到用户所在 LDAP 组: " + roleCode;
            }
            return "RBAC 角色权限已下发到组，待验证用户组成员关系与后端权限刷新: " + roleCode;
        }
        return "RBAC role subset access granted: " + roleCode;
    }

    private boolean supportsRoleSubsetGrant(String cluster, String authBackend, String subjectType) {
        AuthorizationCapability capability = authorizationService.capability(cluster, authBackend);
        if (capability == null || capability.getGrant() == null || !capability.getGrant().isSupportsRoleSubsetGrant()) {
            return false;
        }
        if ("USER".equalsIgnoreCase(subjectType)) {
            return capability.getGrant().isSupportsUserRoleSubsetGrant();
        }
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            return capability.getGrant().isSupportsGroupRoleSubsetGrant();
        }
        return false;
    }

    private boolean assignmentUsesMaterializedPolicies(String cluster, String authBackend, String subjectType) {
        AuthorizationCapability capability = authorizationService.capability(cluster, authBackend);
        if (capability == null || capability.getRbac() == null) {
            return false;
        }
        if (capability.getRbac().isUsesMaterializedPolicies()) {
            return true;
        }
        if ("USER".equalsIgnoreCase(subjectType)) {
            return capability.getRbac().isUserAssignmentUsesMaterializedPolicies();
        }
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            return capability.getRbac().isGroupAssignmentUsesMaterializedPolicies();
        }
        return false;
    }

    private void requireActiveRoleAssignment(String roleCode, String subjectType, String subjectName, String authBackend) {
        if (authRoleService.hasActiveAssignment(roleCode, subjectType, subjectName, authBackend)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该主体尚未绑定角色，不能执行角色范围内授权/回收");
    }

    private boolean usesNativeGroupRole(String cluster, String authBackend) {
        AuthorizationCapability capability = authorizationService.capability(cluster, authBackend);
        return capability != null && capability.getRbac() != null && capability.getRbac().isSupportsNativeGroupRole();
    }

    @SuppressWarnings("unchecked")
    private boolean userBelongsToLdapGroup(String clusterIdentifier, String username, String groupName) {
        if (username == null || groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        try {
            Map<String, Object> profile = ldapService.getUserLdapProfile(clusterIdentifier, username);
            Object primary = profile.get("primaryGroup");
            if (primary instanceof Map && sameText(ldapValue((Map<String, Object>) primary, "name"), groupName)) {
                return true;
            }
            Object supplementary = profile.get("supplementaryGroups");
            if (supplementary instanceof List) {
                for (Object item : (List<?>) supplementary) {
                    if (item instanceof Map && sameText(ldapValue((Map<String, Object>) item, "name"), groupName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("LDAP membership verification failed for " + username + "/" + groupName + ": " + e.getMessage());
        }
        for (DgaUser user : dgaUserRepository.findByUsernameAndIsDeletedFalse(username)) {
            if (sameText(user.getPrimaryGroupName(), groupName)) {
                return true;
            }
            String supplementary = user.getSupplementaryGroups();
            if (supplementary != null) {
                for (String group : supplementary.split(",")) {
                    if (sameText(group, groupName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String revokeRolePermissionSubset(BatchGrantRequest request, String username, String operator,
                                              String cluster, String authBackend, String subjectType, String subjectName) {
        String roleCode = requireText(request.getRoleCode(), "请选择角色");
        String normalizedSubjectType = requireText(subjectType, "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String normalizedSubjectName = requireText(subjectName, "请选择用户或 LDAP 组").trim();
        requireActiveRoleAssignment(roleCode, normalizedSubjectType, normalizedSubjectName, authBackend);
        List<AuthRolePermission> selected = isTableSubsetRequest(request)
                ? validateTableSubsetSelections(roleCode, request.getRolePermissions(), authBackend)
                : validateRolePermissionSelections(roleCode, request.getRolePermissions(), authBackend);
        Map<String, String> sourceRolesByPermissionKey = sourceRolesByPermissionKey(request.getRolePermissions(), authBackend);
        Map<String, String> sourceGroupsByPermissionKey = sourceGroupsByPermissionKey(request.getRolePermissions(), authBackend);
        if ("GROUP".equals(normalizedSubjectType)) {
            if (assignmentUsesMaterializedPolicies(cluster, authBackend, normalizedSubjectType)) {
                for (AuthRolePermission permission : selected) {
                    authorizationService.revokePermissionFromGroup(cluster, normalizedSubjectName,
                            permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(), authBackend);
                }
            } else {
                String fallbackRoleCode = derivedSubsetRoleCode(roleCode, normalizedSubjectType, normalizedSubjectName, selected);
                boolean usedFallbackRole = false;
                for (AuthRolePermission permission : selected) {
                    String targetRoleCode = sourceSubsetRoleForPermission(sourceRolesByPermissionKey,
                            sourceGroupsByPermissionKey, permission, normalizedSubjectName, roleCode);
                    if (targetRoleCode == null) {
                        targetRoleCode = fallbackRoleCode;
                        usedFallbackRole = true;
                    }
                    try {
                        authorizationService.revokePermissionFromRole(cluster, targetRoleCode,
                                permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(), authBackend);
                    } catch (Exception ignored) {
                    }
                }
                if (usedFallbackRole) {
                    authorizationService.revokeRoleAssignment(cluster, fallbackRoleCode, "GROUP", normalizedSubjectName, authBackend);
                }
            }
        } else if ("USER".equals(normalizedSubjectType)) {
            for (AuthRolePermission permission : selected) {
                RevokeCommand command = buildRevokeCommand(normalizedSubjectName, cluster,
                        permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission());
                authorizationService.revoke(command, authBackend);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        for (AuthRolePermission permission : selected) {
            revokeRoleSubsetAccessRecord(cluster, roleCode, normalizedSubjectType, normalizedSubjectName,
                    permission.getDatabaseName(), normalizeAuthTable(permission.getTableName()), permission.getPermission(), operator);
        }
        return "RBAC role subset access revoked: " + roleCode;
    }

    private String forceRevokeUserPermissions(BatchGrantRequest request, String username, String operator,
                                              String cluster, String authBackend) {
        List<BatchGrantRequest.RolePermissionSelection> selections = exactRolePermissionsOrThrow(request.getRolePermissions(), authBackend);
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String permission = requireText(normalizePermissionValue(selection.getPermission()), "请选择权限类型");
            String database = requireText(selection.getDatabaseName(), "请选择数据库");
            String table = normalizeAuthTable(selection.getTableName());
            RevokeCommand command = buildRevokeCommand(username, cluster, database, table, permission);
            authorizationService.revoke(command, authBackend);
            if (table == null) {
                revokeDatabaseAccessRecords(username, cluster, database, permission, operator, "ADMIN_FORCE_USER");
            } else {
                revokeTableAccessRecords(username, cluster, database, table, permission, operator, "ADMIN_FORCE_USER");
            }
        }
        return "Admin force revoke completed for user: " + username;
    }

    private List<AuthRolePermission> validateRolePermissionSelections(String roleCode,
                                                                     List<BatchGrantRequest.RolePermissionSelection> selections,
                                                                     String authBackend) {
        if (selections == null || selections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择至少一项角色权限");
        }
        List<AuthRolePermission> activePermissions = authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE");
        Map<String, AuthRolePermission> activeByKey = new HashMap<>();
        for (AuthRolePermission permission : activePermissions) {
            activeByKey.put(rolePermissionKey(permission.getResourceType(), permission.getDatabaseName(), permission.getTableName(),
                    permission.getPermission(), permission.getAuthBackend()), permission);
        }
        List<AuthRolePermission> selected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String permission = requireText(normalizePermissionValue(selection.getPermission()), "请选择权限类型");
            try {
                AuthorizationSupport.validatePermission(permission);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的权限类型: " + permission, e);
            }
            String key = rolePermissionKey(selection.getResourceType(), selection.getDatabaseName(), selection.getTableName(),
                    permission, firstNonBlank(selection.getAuthBackend(), authBackend));
            AuthRolePermission matched = activeByKey.get(key);
            if (matched == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选权限不属于角色 " + roleCode + "，请刷新角色权限后重试");
            }
            String matchedKey = rolePermissionKey(matched.getResourceType(), matched.getDatabaseName(), matched.getTableName(),
                    matched.getPermission(), matched.getAuthBackend());
            if (seen.add(matchedKey)) {
                selected.add(matched);
            }
        }
        selected.sort(Comparator.comparing(permission -> rolePermissionKey(permission.getResourceType(), permission.getDatabaseName(),
                permission.getTableName(), permission.getPermission(), permission.getAuthBackend())));
        return selected;
    }

    private boolean isTableSubsetRequest(BatchGrantRequest request) {
        if (Boolean.TRUE.equals(request.getTableSubsetMode())) {
            return true;
        }
        List<BatchGrantRequest.RolePermissionSelection> selections = request.getRolePermissions();
        if (selections == null) {
            return false;
        }
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            if (Boolean.TRUE.equals(selection.getExpandedFromDatabasePermission())) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> ALL_SUBSUMES = new HashSet<>(java.util.Arrays.asList(
            "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "INDEX", "LOCK", "READ", "WRITE", "ALL"));

    private boolean permissionSubsumes(String parentPermission, String childPermission) {
        String normalizedParent = upper(parentPermission);
        String normalizedChild = upper(childPermission);
        if (normalizedParent.equals(normalizedChild)) {
            return true;
        }
        if ("ALL".equals(normalizedParent) || "ALL_PRIVILEGES".equals(normalizedParent)) {
            return ALL_SUBSUMES.contains(normalizedChild) || normalizedChild.equals("ALL");
        }
        return false;
    }

    private List<AuthRolePermission> validateTableSubsetSelections(String roleCode,
                                                                   List<BatchGrantRequest.RolePermissionSelection> selections,
                                                                   String authBackend) {
        if (selections == null || selections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择至少一项角色权限");
        }
        List<AuthRolePermission> activePermissions = authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE");
        Map<String, AuthRolePermission> activeByKey = new HashMap<>();
        for (AuthRolePermission permission : activePermissions) {
            activeByKey.put(rolePermissionKey(permission.getResourceType(), permission.getDatabaseName(), permission.getTableName(),
                    permission.getPermission(), permission.getAuthBackend()), permission);
        }
        List<AuthRolePermission> selected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        long virtualIdCounter = 0;
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String permission = requireText(normalizePermissionValue(selection.getPermission()), "请选择权限类型");
            try {
                AuthorizationSupport.validatePermission(permission);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的权限类型: " + permission, e);
            }
            String resolvedAuthBackend = firstNonBlank(selection.getAuthBackend(), authBackend);
            if (Boolean.TRUE.equals(selection.getExpandedFromDatabasePermission())
                    && "TABLE".equalsIgnoreCase(selection.getResourceType())) {
                String databaseName = requireText(selection.getDatabaseName(), "请选择数据库");
                String tableName = requireText(normalizeAuthTable(selection.getTableName()), "请选择表");
                boolean parentFound = false;
                for (AuthRolePermission active : activePermissions) {
                    if (!"DATABASE".equalsIgnoreCase(active.getResourceType())) {
                        continue;
                    }
                    if (!databaseName.equalsIgnoreCase(active.getDatabaseName())) {
                        continue;
                    }
                    String activeBackend = upper(active.getAuthBackend());
                    if (!activeBackend.isEmpty() && !activeBackend.equals(upper(resolvedAuthBackend))) {
                        continue;
                    }
                    if (permissionSubsumes(active.getPermission(), permission)) {
                        parentFound = true;
                        break;
                    }
                }
                if (!parentFound) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "所选表级权限 " + databaseName + "." + tableName + " " + permission
                                    + " 不在角色 " + roleCode + " 的库级权限范围内，请刷新角色权限后重试");
                }
                String virtualKey = rolePermissionKey("TABLE", databaseName, tableName, permission, resolvedAuthBackend);
                if (seen.add(virtualKey)) {
                    virtualIdCounter++;
                    AuthRolePermission virtual = new AuthRolePermission();
                    virtual.setId(-1L * virtualIdCounter);
                    virtual.setRoleCode(roleCode);
                    virtual.setResourceType("TABLE");
                    virtual.setDatabaseName(databaseName);
                    virtual.setTableName(tableName);
                    virtual.setPermission(permission);
                    virtual.setAuthBackend(resolvedAuthBackend);
                    virtual.setStatus("ACTIVE");
                    selected.add(virtual);
                }
            } else {
                String key = rolePermissionKey(selection.getResourceType(), selection.getDatabaseName(), selection.getTableName(),
                        permission, resolvedAuthBackend);
                AuthRolePermission matched = activeByKey.get(key);
                if (matched == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "所选权限不属于角色 " + roleCode + "，请刷新角色权限后重试");
                }
                String matchedKey = rolePermissionKey(matched.getResourceType(), matched.getDatabaseName(), matched.getTableName(),
                        matched.getPermission(), matched.getAuthBackend());
                if (seen.add(matchedKey)) {
                    selected.add(matched);
                }
            }
        }
        selected.sort(Comparator.comparing(p -> rolePermissionKey(p.getResourceType(), p.getDatabaseName(),
                p.getTableName(), p.getPermission(), p.getAuthBackend())));
        return selected;
    }

    private List<BatchGrantRequest.RolePermissionSelection> exactRolePermissionsOrThrow(
            List<BatchGrantRequest.RolePermissionSelection> selections, String authBackend) {
        if (selections == null || selections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一项权限");
        }
        List<BatchGrantRequest.RolePermissionSelection> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String permission = requireText(normalizePermissionValue(selection.getPermission()), "请选择权限类型");
            AuthorizationSupport.validatePermission(permission);
            String database = requireText(selection.getDatabaseName(), "请选择数据库");
            String table = normalizeAuthTable(selection.getTableName());
            String key = rolePermissionKey(selection.getResourceType(), database, table, permission,
                    firstNonBlank(selection.getAuthBackend(), authBackend));
            if (!seen.add(key)) {
                continue;
            }
            BatchGrantRequest.RolePermissionSelection item = new BatchGrantRequest.RolePermissionSelection();
            item.setResourceType(firstNonBlank(selection.getResourceType(), table == null ? "DATABASE" : "TABLE"));
            item.setDatabaseName(database);
            item.setTableName(table);
            item.setPermission(permission);
            item.setAuthBackend(firstNonBlank(selection.getAuthBackend(), authBackend));
            normalized.add(item);
        }
        return normalized;
    }

    private String rolePermissionKey(String resourceType, String databaseName, String tableName, String permission, String authBackend) {
        return String.join("|",
                upper(firstNonBlank(resourceType, normalizeAuthTable(tableName) == null ? "DATABASE" : "TABLE")),
                lower(databaseName),
                lower(firstNonBlank(normalizeAuthTable(tableName), "*")),
                upper(permission),
                upper(authBackend));
    }

    private Map<String, String> sourceRolesByPermissionKey(List<BatchGrantRequest.RolePermissionSelection> selections, String authBackend) {
        Map<String, String> sourceRoles = new HashMap<>();
        if (selections == null) {
            return sourceRoles;
        }
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String sourceRole = firstNonBlank(selection.getSourceRole());
            if (sourceRole == null) {
                continue;
            }
            String permission = normalizePermissionValue(selection.getPermission());
            String key = rolePermissionKey(selection.getResourceType(), selection.getDatabaseName(), selection.getTableName(),
                    permission, firstNonBlank(selection.getAuthBackend(), authBackend));
            sourceRoles.put(key, sourceRole);
        }
        return sourceRoles;
    }

    private Map<String, String> sourceGroupsByPermissionKey(List<BatchGrantRequest.RolePermissionSelection> selections, String authBackend) {
        Map<String, String> sourceGroups = new HashMap<>();
        if (selections == null) {
            return sourceGroups;
        }
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            String sourceGroup = firstNonBlank(selection.getSourceGroup());
            if (sourceGroup == null) {
                continue;
            }
            String permission = normalizePermissionValue(selection.getPermission());
            String key = rolePermissionKey(selection.getResourceType(), selection.getDatabaseName(), selection.getTableName(),
                    permission, firstNonBlank(selection.getAuthBackend(), authBackend));
            sourceGroups.put(key, sourceGroup);
        }
        return sourceGroups;
    }

    private String sourceSubsetRoleForPermission(Map<String, String> sourceRolesByPermissionKey,
                                                 Map<String, String> sourceGroupsByPermissionKey,
                                                 AuthRolePermission permission,
                                                 String subjectName,
                                                 String roleCode) {
        String key = rolePermissionKey(permission.getResourceType(), permission.getDatabaseName(), permission.getTableName(),
                permission.getPermission(), permission.getAuthBackend());
        String sourceRole = firstNonBlank(sourceRolesByPermissionKey.get(key));
        if (sourceRole == null) {
            return null;
        }
        String sourceGroup = firstNonBlank(sourceGroupsByPermissionKey.get(key));
        if (sourceRole.toLowerCase(Locale.ROOT).startsWith(roleSubsetPrefix(roleCode)) || sameText(sourceGroup, subjectName)) {
            return sourceRole;
        }
        return null;
    }

    private String derivedSubsetRoleCode(String roleCode, String subjectType, String subjectName, List<AuthRolePermission> permissions) {
        String seed = roleCode + "|" + subjectType + "|" + subjectName + "|" + permissions.stream()
                .map(permission -> rolePermissionKey(permission.getResourceType(), permission.getDatabaseName(), permission.getTableName(),
                        permission.getPermission(), permission.getAuthBackend()))
                .sorted()
                .collect(Collectors.joining(";"));
        String hash = Integer.toHexString(seed.hashCode());
        return roleSubsetPrefix(roleCode) + hash;
    }

    private String roleSubsetPrefix(String roleCode) {
        String prefix = roleCode == null ? "role_subset" : roleCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (prefix.length() > 44) {
            prefix = prefix.substring(0, 44);
        }
        return prefix + "_sub_";
    }

    private void revokeRoleSubsetAccessRecord(String cluster, String roleCode, String subjectType, String subjectName,
                                              String database, String table, String permission, String operator) {
        int updated;
        if (table == null) {
            updated = userResourceAccessRepository.softDeleteRoleSubsetDatabaseAccess(
                    cluster, database, permission, roleCode, subjectType, subjectName, operator);
        } else {
            updated = userResourceAccessRepository.softDeleteRoleSubsetTableAccess(
                    cluster, database, table, permission, roleCode, subjectType, subjectName, operator);
        }
        if (updated == 0) {
            saveResourceAccess(subjectName,
                    cluster, database, table, permission, operator, "RBAC_ROLE_SUBSET", "REVOKED", true, LocalDateTime.now(),
                    "ROLE", roleCode, subjectType, subjectName, null, null, null, null, riskLevelFor(permission), null);
        }
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    private List<String> resolvePermissionsOrThrow(BatchGrantRequest request) {
        Set<String> normalized = new LinkedHashSet<>();
        if (request.getPermissions() != null) {
            for (String permission : request.getPermissions()) {
                addNormalizedPermission(normalized, permission);
            }
        }
        addNormalizedPermission(normalized, request.getPermission());
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择权限类型");
        }
        return new ArrayList<>(normalized);
    }

    private void addNormalizedPermission(Set<String> target, String permission) {
        if (permission == null) {
            return;
        }
        for (String part : permission.split(",")) {
            String normalized = normalizePermissionValue(part);
            if (normalized != null && !normalized.isEmpty()) {
                try {
                    AuthorizationSupport.validatePermission(normalized);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的权限类型: " + normalized, e);
                }
                target.add(normalized);
            }
        }
    }

    private void validateDirectException(BatchGrantRequest request, String cluster, String authBackend, String username) {
        String subjectType = firstNonBlank(request.getSubjectType(), "USER").toUpperCase(Locale.ROOT);
        if (!"USER".equals(subjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "直接例外授权仅支持 USER 主体");
        }
        requireText(username, "请选择用户");
        requireText(request.getExceptionReason(), "临时直授必须填写授权理由");
        requireText(request.getTicketNo(), "临时直授必须填写工单号");
        requireText(request.getApprover(), "临时直授必须填写审批人");
        if (parseOptionalDateTime(request.getExpiresAt()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时直授必须填写过期时间");
        }
        if (!isHistoricalUserPrincipal(username, cluster, authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "新用户必须先绑定角色后再授权；仅历史已有用户允许直接例外授权/回收");
        }
    }

    private boolean isHistoricalUserPrincipal(String username, String cluster, String authBackend) {
        String normalizedUsername = firstNonBlank(username);
        if (normalizedUsername == null) {
            return false;
        }
        boolean existsInBackend;
        try {
            existsInBackend = authorizationService.userExists(cluster, normalizedUsername, authBackend);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readablePrincipalListError(e), e);
        }
        if (authorizationService.requiresExistingBackendUser(cluster, authBackend) && !existsInBackend) {
            return false;
        }
        return hasRecordedAccess(normalizedUsername, cluster) || existsInBackend;
    }

    private boolean hasRecordedAccess(String username, String cluster) {
        String resolvedCluster = authorizationService.resolveClusterCodeOrName(cluster);
        for (UserResourceAccess access : userResourceAccessRepository.findByUsernameAndIsDeletedFalse(username)) {
            if (resolvedCluster == null || resolvedCluster.trim().isEmpty()) {
                return true;
            }
            if (sameText(resolvedCluster, access.getClusterCode())
                    || sameText(resolvedCluster, access.getClusterName())
                    || sameText(cluster, access.getClusterCode())
                    || sameText(cluster, access.getClusterName())) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveStarRocksInventoryUsernames(StarRocksPermissionInventoryRequest request,
                                                            String cluster,
                                                            String resolvedCluster,
                                                            String authBackend) {
        int maxUsers = normalizeInventoryMaxUsers(request.getMaxUsers());
        String keyword = lower(request.getKeyword());
        Set<String> unique = new LinkedHashSet<>();
        if (request.getUsernames() != null) {
            for (String item : request.getUsernames()) {
                String username = firstNonBlank(item);
                if (username != null) {
                    unique.add(username);
                }
            }
        }
        if (!unique.isEmpty()) {
            List<String> selected = new ArrayList<>(unique);
            selected.sort(String.CASE_INSENSITIVE_ORDER);
            return selected.size() > maxUsers ? selected.subList(0, maxUsers) : selected;
        }
        List<String> principals;
        try {
            principals = authorizationService.listPrincipals(cluster, authBackend);
        } catch (Exception e) {
            List<String> recordedFallback = recordedInventoryUsernames(resolvedCluster, cluster, authBackend, keyword);
            if (!recordedFallback.isEmpty()) {
                return recordedFallback.size() > maxUsers ? recordedFallback.subList(0, maxUsers) : recordedFallback;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    readablePrincipalListError(e) + "；当前没有可兜底的 recorded 权限用户，请先同步 StarRocks 用户或补齐权限记录后重试", e);
        }
        if (principals != null) {
            for (String principal : principals) {
                String username = firstNonBlank(principal);
                if (username == null) {
                    continue;
                }
                if (!keyword.isEmpty() && !lower(username).contains(keyword)) {
                    continue;
                }
                unique.add(username);
            }
        }
        for (String username : recordedInventoryUsernames(resolvedCluster, cluster, authBackend, keyword)) {
            unique.add(username);
        }
        List<String> selected = new ArrayList<>(unique);
        selected.sort(String.CASE_INSENSITIVE_ORDER);
        return selected.size() > maxUsers ? selected.subList(0, maxUsers) : selected;
    }

    private List<String> recordedInventoryUsernames(String resolvedCluster,
                                                    String cluster,
                                                    String authBackend,
                                                    String keyword) {
        String targetCluster = firstNonBlank(resolvedCluster, cluster);
        String normalizedAuthBackend = authorizationService.normalizeAuthBackend(authBackend);
        Set<String> usernames = new LinkedHashSet<>();
        for (UserResourceAccess access : userResourceAccessRepository.findActiveByCluster(targetCluster)) {
            String username = firstNonBlank(access == null ? null : access.getUsername());
            if (username == null) {
                continue;
            }
            if (!keyword.isEmpty() && !lower(username).contains(keyword)) {
                continue;
            }
            String recordedBackend = authorizationService.normalizeAuthBackend(access.getAuthBackend());
            if (normalizedAuthBackend != null && recordedBackend != null && !normalizedAuthBackend.equals(recordedBackend)) {
                continue;
            }
            usernames.add(username);
        }
        List<String> selected = new ArrayList<>(usernames);
        selected.sort(String.CASE_INSENSITIVE_ORDER);
        return selected;
    }

    private int normalizeInventoryMaxUsers(Integer value) {
        if (value == null) {
            return 50;
        }
        if (value < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxUsers 必须大于 0");
        }
        if (value > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxUsers 不能超过 200");
        }
        return value;
    }

    private Map<String, Object> buildStarRocksInventoryUser(String username,
                                                            String cluster,
                                                            String resolvedCluster,
                                                            String authBackend,
                                                            boolean includeRecordedGrants) {
        List<Map<String, Object>> grants = dedupePermissionRows(
                normalizePermissionRows(authorizationService.getUserPermissions(username, cluster, authBackend)),
                authBackend);
        List<Map<String, Object>> recordedGrants = new ArrayList<>();
        if (includeRecordedGrants) {
            List<UserResourceAccess> recordedAccess = resolvedCluster == null || resolvedCluster.trim().isEmpty()
                    ? userResourceAccessRepository.findByUsernameAndIsDeletedFalse(username)
                    : userResourceAccessRepository.findByUsernameAndClusterCodeAndIsDeletedFalse(username, resolvedCluster);
            recordedGrants = dedupePermissionRows(normalizeRecordedPermissionRows(recordedAccess), authBackend);
        }

        Set<String> liveKeys = permissionRowKeys(grants, authBackend);
        Set<String> recordedKeys = permissionRowKeys(recordedGrants, authBackend);
        int matchedRecordedCount = 0;
        for (String key : liveKeys) {
            if (recordedKeys.contains(key)) {
                matchedRecordedCount++;
            }
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("username", username);
        item.put("grantCount", grants.size());
        item.put("recordedGrantCount", recordedGrants.size());
        item.put("matchedRecordedCount", matchedRecordedCount);
        item.put("liveOnlyCount", Math.max(liveKeys.size() - matchedRecordedCount, 0));
        item.put("recordedOnlyCount", Math.max(recordedKeys.size() - matchedRecordedCount, 0));
        item.put("highRiskGrantCount", countHighRiskGrants(grants));
        item.put("permissionCategory", templateCategory(grants));
        item.put("permissionSignature", permissionSignature(grants, authBackend));
        item.put("permissionSummary", permissionSummary(grants));
        item.put("grants", grants);
        item.put("recordedGrants", recordedGrants);
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildStarRocksTemplateCandidates(List<Map<String, Object>> users) {
        Map<String, List<Map<String, Object>>> usersBySignature = new LinkedHashMap<>();
        for (Map<String, Object> user : users) {
            String signature = String.valueOf(user.get("permissionSignature"));
            usersBySignature.computeIfAbsent(signature, key -> new ArrayList<>()).add(user);
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, List<Map<String, Object>>> entry : usersBySignature.entrySet()) {
            List<Map<String, Object>> groupedUsers = entry.getValue();
            if (groupedUsers.isEmpty()) {
                continue;
            }
            groupedUsers.sort(Comparator.comparing(item -> String.valueOf(item.get("username")), String.CASE_INSENSITIVE_ORDER));
            Map<String, Object> first = groupedUsers.get(0);
            List<Map<String, Object>> grants = (List<Map<String, Object>>) first.get("grants");
            List<String> usernames = new ArrayList<>();
            for (Map<String, Object> groupedUser : groupedUsers) {
                usernames.add(String.valueOf(groupedUser.get("username")));
            }

            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("candidateNo", index);
            candidate.put("templateCodeSuggestion", suggestTemplateCode(grants, index));
            candidate.put("permissionSignature", entry.getKey());
            candidate.put("permissionCategory", first.get("permissionCategory"));
            candidate.put("userCount", groupedUsers.size());
            candidate.put("grantCount", grants == null ? 0 : grants.size());
            candidate.put("highRiskGrantCount", first.get("highRiskGrantCount"));
            candidate.put("usernames", usernames);
            candidate.put("permissionSummary", first.get("permissionSummary"));
            candidate.put("grants", grants);
            candidates.add(candidate);
            index++;
        }
        candidates.sort(Comparator
                .comparing((Map<String, Object> item) -> ((Number) item.get("userCount")).intValue()).reversed()
                .thenComparing(item -> String.valueOf(item.get("templateCodeSuggestion")), String.CASE_INSENSITIVE_ORDER));
        return candidates;
    }

    private List<Map<String, Object>> dedupePermissionRows(List<Map<String, Object>> rows, String authBackend) {
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        if (rows == null) {
            return new ArrayList<>();
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> compact = compactPermissionRow(row);
            String key = permissionRowKey(compact, authBackend);
            if (!unique.containsKey(key)) {
                unique.put(key, compact);
            }
        }
        List<Map<String, Object>> normalized = new ArrayList<>(unique.values());
        normalized.sort(Comparator.comparing(item -> permissionRowKey(item, authBackend)));
        int index = 1;
        for (Map<String, Object> item : normalized) {
            item.put("id", index++);
        }
        return normalized;
    }

    private Map<String, Object> compactPermissionRow(Map<String, Object> row) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("resourceType", firstNonBlank(stringValue(row.get("resourceType")), "DATABASE"));
        compact.put("databaseName", stringValue(row.get("databaseName")));
        compact.put("tableName", normalizeAuthTable(stringValue(row.get("tableName"))));
        compact.put("permission", firstNonBlank(normalizePermissionValue(stringValue(row.get("permission"))), "-"));
        compact.put("grantText", stringValue(row.get("grantText")));
        compact.put("source", stringValue(row.get("source")));
        compact.put("sourceRole", stringValue(row.get("sourceRole")));
        compact.put("sourceGroup", stringValue(row.get("sourceGroup")));
        compact.put("status", stringValue(row.get("status")));
        compact.put("grantMode", stringValue(row.get("grantMode")));
        compact.put("roleCode", stringValue(row.get("roleCode")));
        compact.put("subjectType", stringValue(row.get("subjectType")));
        compact.put("subjectName", stringValue(row.get("subjectName")));
        return compact;
    }

    private Set<String> permissionRowKeys(List<Map<String, Object>> rows, String authBackend) {
        Set<String> keys = new LinkedHashSet<>();
        if (rows == null) {
            return keys;
        }
        for (Map<String, Object> row : rows) {
            keys.add(permissionRowKey(row, authBackend));
        }
        return keys;
    }

    private String permissionRowKey(Map<String, Object> row, String authBackend) {
        return rolePermissionKey(
                stringValue(row.get("resourceType")),
                stringValue(row.get("databaseName")),
                stringValue(row.get("tableName")),
                stringValue(row.get("permission")),
                authBackend);
    }

    private int countHighRiskGrants(List<Map<String, Object>> grants) {
        int count = 0;
        if (grants == null) {
            return count;
        }
        for (Map<String, Object> grant : grants) {
            if (isHighRiskPermission(stringValue(grant.get("permission")))) {
                count++;
            }
        }
        return count;
    }

    private List<String> permissionSummary(List<Map<String, Object>> grants) {
        List<String> summary = new ArrayList<>();
        if (grants == null) {
            return summary;
        }
        for (Map<String, Object> grant : grants) {
            summary.add(resourceSummary(
                    stringValue(grant.get("resourceType")),
                    stringValue(grant.get("databaseName")),
                    stringValue(grant.get("tableName")),
                    stringValue(grant.get("permission"))));
        }
        return summary;
    }

    private String templateCategory(List<Map<String, Object>> grants) {
        if (grants == null || grants.isEmpty()) {
            return "EMPTY";
        }
        boolean hasAdmin = false;
        boolean hasWrite = false;
        boolean hasRead = false;
        for (Map<String, Object> grant : grants) {
            String permission = upper(stringValue(grant.get("permission")));
            if ("ALL".equals(permission) || "DROP".equals(permission) || "ALTER".equals(permission) || "CREATE".equals(permission)) {
                hasAdmin = true;
            }
            if ("INSERT".equals(permission)) {
                hasWrite = true;
            }
            if ("SELECT".equals(permission)) {
                hasRead = true;
            }
        }
        if (hasAdmin) {
            return "ADMIN";
        }
        if (hasWrite && hasRead) {
            return "READ_WRITE";
        }
        if (hasWrite) {
            return "WRITE";
        }
        if (hasRead) {
            return "READONLY";
        }
        return "MIXED";
    }

    private String permissionSignature(List<Map<String, Object>> grants, String authBackend) {
        List<String> keys = new ArrayList<>(permissionRowKeys(grants, authBackend));
        java.util.Collections.sort(keys);
        return sha256Hex(String.join("||", keys));
    }

    private String suggestTemplateCode(List<Map<String, Object>> grants, int index) {
        Set<String> databases = new LinkedHashSet<>();
        if (grants != null) {
            for (Map<String, Object> grant : grants) {
                String database = firstNonBlank(stringValue(grant.get("databaseName")));
                if (database != null && !"ALL DATABASES".equalsIgnoreCase(database) && !"SYSTEM".equalsIgnoreCase(database)) {
                    databases.add(slugToken(database));
                }
            }
        }
        String scope = databases.isEmpty() ? "global" : (databases.size() == 1 ? databases.iterator().next() : "mixed");
        return String.format("sr_%s_%s_%03d", scope, templateCategory(grants).toLowerCase(Locale.ROOT), index);
    }

    private String slugToken(String value) {
        String normalized = lower(value).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            return "scope";
        }
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private boolean isHighRiskPermission(String permission) {
        if (permission == null) return false;
        String normalized = permission.trim().toUpperCase();
        return "ALL".equals(normalized)
                || "ADMIN".equals(normalized)
                || "CREATE".equals(normalized)
                || "DROP".equals(normalized)
                || "INSERT".equals(normalized);
    }

    private String riskLevelFor(String permission) {
        return isHighRiskPermission(permission) ? "HIGH" : "LOW";
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "过期时间格式必须为 yyyy-MM-ddTHH:mm:ss", e);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizePermissionValue(String permission) {
        if (permission == null) {
            return null;
        }
        String normalized = permission.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith("_PRIV")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        if ("*".equals(normalized) || "ALL PRIVILEGES".equals(normalized) || "ALL_PRIVILEGES".equals(normalized)) {
            return "ALL";
        }
        return normalized;
    }

    private String readableAuthorizationError(Exception e) {
        String message = e.getMessage() == null ? "授权执行失败" : e.getMessage();
        String compactMessage = compactExceptionMessage(message);
        String lower = message.toLowerCase();
        if (lower.contains("sentrynosuchobjectexception") && lower.contains("role")
                && (lower.contains("doesn't exist") || lower.contains("does not exist"))) {
            return "授权后端角色不存在，请先同步或创建 Sentry 角色后再重试";
        }
        if (lower.contains("invalid permission")) {
            return "权限类型不被当前授权后端支持，请只选择该后端支持的资源权限后重试";
        }
        if (lower.contains("access denied") || lower.contains("denied")) {
            return "授权端点账号权限不足: " + compactMessage;
        }
        if (lower.contains("syntax") || lower.contains("sql")) {
            return "授权 SQL 执行失败: " + compactMessage;
        }
        return compactMessage;
    }

    private String compactExceptionMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "授权执行失败";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        compact = compact.replaceAll("(?i)Server Stacktrace:.*$", "").trim();
        compact = compact.replaceAll("(?i);\\s*nested exception is.*$", "").trim();
        compact = compact.replaceAll("；\\s*根因:.*$", "").trim();
        return compact.length() > 240 ? compact.substring(0, 240) + "..." : compact;
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

    private List<Map<String, Object>> normalizeRecordedPermissionRows(List<UserResourceAccess> recordedAccess) {
        List<Map<String, Object>> grants = new ArrayList<>();
        if (recordedAccess == null) {
            return grants;
        }
        int index = 1;
        for (UserResourceAccess access : recordedAccess) {
            if (access == null || Boolean.TRUE.equals(access.getDeleted()) || !"ACTIVE".equalsIgnoreCase(access.getStatus())) {
                continue;
            }
            Map<String, Object> grant = new HashMap<>();
            grant.put("id", "recorded-" + index++);
            grant.put("resourceType", firstNonBlank(access.getResourceType(), access.getTableName() == null ? "DATABASE" : "TABLE"));
            grant.put("databaseName", access.getDatabaseName());
            grant.put("tableName", normalizeAuthTable(access.getTableName()));
            grant.put("permission", firstNonBlank(normalizePermissionValue(access.getPermission()), "-"));
            grant.put("grantText", recordedPermissionText(access));
            grant.put("source", access.getSource());
            grant.put("status", access.getStatus());
            grant.put("grantMode", access.getGrantMode());
            grant.put("roleCode", access.getRoleCode());
            grant.put("subjectType", access.getSubjectType());
            grant.put("subjectName", access.getSubjectName());
            grants.add(grant);
        }
        return grants;
    }

    private String recordedPermissionText(UserResourceAccess access) {
        StringBuilder text = new StringBuilder("DGA recorded");
        if (access.getRoleCode() != null && !access.getRoleCode().trim().isEmpty()) {
            text.append(" role=").append(access.getRoleCode());
        }
        if (access.getSubjectType() != null && access.getSubjectName() != null) {
            text.append(" subject=").append(access.getSubjectType()).append('/').append(access.getSubjectName());
        }
        if (access.getSource() != null && !access.getSource().trim().isEmpty()) {
            text.append(" source=").append(access.getSource());
        }
        return text.toString();
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
        permission = normalizePermissionValue(permission);
        table = normalizeAuthTable(table);
        if (table == null && !"GLOBAL".equals(resourceType)) {
            resourceType = "DATABASE";
        }

        Map<String, Object> grant = new HashMap<>();
        grant.put("resourceType", resourceType);
        grant.put("databaseName", database);
        grant.put("tableName", table);
        grant.put("permission", permission != null ? permission.toUpperCase() : "-");
        grant.put("grantText", grantText);
        grant.put("source", pickString(lowerRow, "source"));
        grant.put("sourceRole", pickString(lowerRow, "sourceRole", "source_role"));
        grant.put("sourceGroup", pickString(lowerRow, "sourceGroup", "source_group"));
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
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

    private String currentOperator() {
        return CurrentUser.usernameOrUnknown();
    }

    private void revokeDatabaseAccessRecords(String username, String cluster, String database,
                                             String permission, String operator, String source) {
        userHiveAccessRepository.softDeleteDatabaseAccess(username, cluster, database, permission);
        int updated = userResourceAccessRepository.softDeleteDatabaseAccess(
                username, cluster, database, permission, operator);
        if (updated == 0) {
            saveResourceAccess(username, cluster, database, null, permission,
                    operator, source, "REVOKED", true, LocalDateTime.now());
        }
    }

    private void revokeTableAccessRecords(String username, String cluster, String database, String table,
                                          String permission, String operator, String source) {
        userHiveAccessRepository.softDeleteTableAccess(username, cluster, database, table, permission);
        int updated = userResourceAccessRepository.softDeleteTableAccess(
                username, cluster, database, table, permission, operator);
        if (updated == 0) {
            saveResourceAccess(username, cluster, database, table, permission,
                    operator, source, "REVOKED", true, LocalDateTime.now());
        }
    }

    private void saveResourceAccess(String username, String cluster, String database, String table,
                                    String permission, String grantedBy, String source) {
        saveResourceAccess(username, cluster, database, table, permission,
                grantedBy, source, "ACTIVE", false, null);
    }

    private void saveResourceAccess(String username, String cluster, String database, String table,
                                    String permission, String operator, String source,
                                    String status, boolean deleted, LocalDateTime revokeTime) {
        saveResourceAccess(username, cluster, database, table, permission, operator, source, status, deleted, revokeTime,
                null, null, null, null, null, null, null);
    }

    private void saveResourceAccess(String username, String cluster, String database, String table,
                                    String permission, String operator, String source,
                                    String status, boolean deleted, LocalDateTime revokeTime,
                                    String grantMode, String roleCode, String exceptionReason,
                                    String ticketNo, String approver, LocalDateTime expiresAt, String riskLevel) {
        saveResourceAccess(username, cluster, database, table, permission, operator, source, status, deleted, revokeTime,
                grantMode, roleCode, "USER", username, exceptionReason, ticketNo, approver, expiresAt, riskLevel, null);
    }

    private void saveResourceAccess(String username, String cluster, String database, String table,
                                    String permission, String operator, String source,
                                    String status, boolean deleted, LocalDateTime revokeTime,
                                    String grantMode, String roleCode, String subjectType, String subjectName,
                                    String exceptionReason, String ticketNo, String approver,
                                    LocalDateTime expiresAt, String riskLevel, String authBackend) {
        String clusterIdentifier = cluster != null && !cluster.isEmpty() ? cluster : "CDH-Cluster-01";
        String normalizedTable = normalizeAuthTable(table);
        String resolvedAuthBackend = authorizationService.normalizeAuthBackend(authBackend);
        UserResourceAccess access = new UserResourceAccess();
        access.setUsername(username);
        access.setClusterName(clusterIdentifier);
        access.setClusterCode(authorizationService.resolveClusterCodeOrName(clusterIdentifier));
        access.setEngineType(resolvedAuthBackend == null
                ? authorizationService.engineType(clusterIdentifier)
                : authorizationService.engineType(clusterIdentifier, resolvedAuthBackend));
        access.setAuthBackend(resolvedAuthBackend == null
                ? authorizationService.authBackend(clusterIdentifier)
                : resolvedAuthBackend);
        access.setResourceType(normalizedTable == null ? "DATABASE" : "TABLE");
        access.setDatabaseName(database);
        access.setTableName(normalizedTable);
        access.setPermission(permission);
        access.setGrantMode(grantMode);
        access.setRoleCode(roleCode);
        access.setSubjectType(firstNonBlank(subjectType, "USER").toUpperCase());
        access.setSubjectName(firstNonBlank(subjectName, username));
        access.setExceptionReason(exceptionReason);
        access.setTicketNo(ticketNo);
        access.setApprover(approver);
        access.setExpiresAt(expiresAt);
        access.setRiskLevel(riskLevel);
        access.setGrantedBy(operator);
        access.setSource(source);
        access.setStatus(status);
        access.setDeleted(deleted);
        if ("REVOKED".equals(status)) {
            access.setRevokedBy(operator);
            access.setRevokeTime(revokeTime != null ? revokeTime : LocalDateTime.now());
        } else {
            access.setGrantTime(LocalDateTime.now());
        }
        userResourceAccessRepository.save(access);
    }

    private Map<String, Object> permissionAuditRow(UserResourceAccess access) {
        Map<String, Object> row = new LinkedHashMap<>();
        boolean revoked = "REVOKED".equalsIgnoreCase(access.getStatus()) || Boolean.TRUE.equals(access.getDeleted());
        LocalDateTime time = revoked && access.getRevokeTime() != null ? access.getRevokeTime() : access.getGrantTime();
        row.put("key", "permission-" + access.getId() + (revoked ? "-revoke" : "-grant"));
        row.put("type", revoked ? "PERMISSION_REVOKE" : "PERMISSION_GRANT");
        row.put("title", permissionAuditTitle(access, revoked));
        row.put("time", time);
        row.put("timeValue", time == null ? "" : time.toString());
        row.put("operator", revoked ? access.getRevokedBy() : access.getGrantedBy());
        row.put("cluster", firstNonBlank(access.getClusterCode(), access.getClusterName()));
        row.put("description", permissionAuditDescription(access));
        row.put("color", revoked ? "red" : "green");
        row.put("status", access.getStatus());
        row.put("roleCode", access.getRoleCode());
        row.put("source", access.getSource());
        return row;
    }

    private Map<String, Object> roleAssignmentAuditRow(AuthRoleAssignmentAudit audit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", "role-audit-" + audit.getId());
        row.put("type", "ROLE_ASSIGNMENT");
        row.put("title", roleAssignmentAuditTitle(audit));
        row.put("time", audit.getActionTime());
        row.put("timeValue", audit.getActionTime() == null ? "" : audit.getActionTime().toString());
        row.put("operator", audit.getOperator());
        row.put("cluster", audit.getCluster());
        row.put("description", roleAssignmentAuditDescription(audit));
        row.put("color", roleAssignmentAuditColor(audit));
        row.put("status", audit.getBackendStatus());
        row.put("roleCode", audit.getRoleCode());
        row.put("source", audit.getAction());
        return row;
    }

    private String permissionAuditTitle(UserResourceAccess access, boolean revoked) {
        String mode = String.valueOf(firstNonBlank(access.getGrantMode(), access.getSource(), "DIRECT"));
        if (mode.contains("ROLE") || access.getRoleCode() != null) {
            return revoked ? "角色权限回收" : "角色权限授权";
        }
        return revoked ? "权限回收" : "权限授权";
    }

    private String permissionAuditDescription(UserResourceAccess access) {
        List<String> parts = new ArrayList<>();
        parts.add(resourceSummary(access.getResourceType(), access.getDatabaseName(), access.getTableName(), access.getPermission()));
        if (access.getRoleCode() != null && !access.getRoleCode().trim().isEmpty()) {
            parts.add("角色 " + access.getRoleCode());
        }
        if (access.getSubjectName() != null && !access.getSubjectName().trim().isEmpty()) {
            parts.add(String.valueOf(access.getSubjectType()).toUpperCase(Locale.ROOT) + " " + access.getSubjectName());
        }
        if (access.getAuthBackend() != null && !access.getAuthBackend().trim().isEmpty()) {
            parts.add(access.getAuthBackend());
        }
        return String.join("，", parts);
    }

    private String roleAssignmentAuditTitle(AuthRoleAssignmentAudit audit) {
        String action = String.valueOf(audit.getAction()).toUpperCase(Locale.ROOT);
        if (action.contains("REMOVE") || action.contains("DELETE") || action.contains("REVOKE")) {
            return "角色绑定移除";
        }
        if (action.contains("GRANT") || action.contains("ASSIGN") || action.contains("ADD")) {
            return "角色绑定";
        }
        return "角色绑定变更";
    }

    private String roleAssignmentAuditDescription(AuthRoleAssignmentAudit audit) {
        List<String> parts = new ArrayList<>();
        if (audit.getRoleCode() != null && !audit.getRoleCode().trim().isEmpty()) {
            parts.add("角色 " + audit.getRoleCode());
        }
        if (audit.getSubjectName() != null && !audit.getSubjectName().trim().isEmpty()) {
            parts.add(String.valueOf(audit.getSubjectType()).toUpperCase(Locale.ROOT) + " " + audit.getSubjectName());
        }
        if (audit.getResourceSummary() != null && !audit.getResourceSummary().trim().isEmpty()) {
            parts.add(audit.getResourceSummary());
        }
        if (audit.getMessage() != null && !audit.getMessage().trim().isEmpty()) {
            parts.add(audit.getMessage());
        }
        return String.join("，", parts);
    }

    private String roleAssignmentAuditColor(AuthRoleAssignmentAudit audit) {
        String action = String.valueOf(audit.getAction()).toUpperCase(Locale.ROOT);
        if (action.contains("REMOVE") || action.contains("DELETE") || action.contains("REVOKE")) {
            return "red";
        }
        return "purple";
    }

    private String resourceSummary(String resourceType, String databaseName, String tableName, String permission) {
        String type = String.valueOf(firstNonBlank(resourceType, tableName == null ? "DATABASE" : "TABLE")).toUpperCase(Locale.ROOT);
        String scope = "TABLE".equals(type)
                ? String.valueOf(firstNonBlank(databaseName, "-")) + "." + String.valueOf(firstNonBlank(tableName, "*"))
                : String.valueOf(firstNonBlank(databaseName, "ALL DATABASES"));
        return type + " " + scope + " " + String.valueOf(firstNonBlank(permission, "-"));
    }

    private String normalizeAuthTable(String table) {
        if (table == null) {
            return null;
        }
        String normalized = table.trim();
        if (normalized.isEmpty() || "*".equals(normalized) || "ALL TABLES".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeGovernanceUserType(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return "INTERNAL";
        }
        String normalized = userType.trim().toUpperCase();
        if (!java.util.Arrays.asList("INTERNAL", "OUTSOURCER", "TEMPORARY", "SERVICE").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的用户类型: " + userType);
        }
        return normalized;
    }

    private void validateUserExpiry(String userType, LocalDateTime expiresAt) {
        boolean requiresExpiry = "OUTSOURCER".equals(userType) || "TEMPORARY".equals(userType);
        if (requiresExpiry && expiresAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "外包/临时用户必须设置过期时间");
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "过期时间必须晚于当前时间");
        }
    }
}
