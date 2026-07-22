package com.dga.access.service;

import com.dga.access.dto.AuthRoleAssignmentRequest;
import com.dga.access.dto.AuthRoleImportRequest;
import com.dga.access.dto.AuthRoleImportResult;
import com.dga.access.dto.AuthRolePermissionRequest;
import com.dga.access.dto.AuthRoleRequest;
import com.dga.access.dto.AuthRoleView;
import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRolePermissionSnapshot;
import com.dga.access.dto.BackendRoleSnapshot;
import com.dga.access.dto.BatchRoleAssignmentItem;
import com.dga.access.dto.BatchRoleAssignmentRequest;
import com.dga.access.dto.BatchRoleAssignmentResult;
import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthRoleAssignmentAudit;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.entity.AuthUserRole;
import com.dga.access.repository.AuthRoleAssignmentAuditRepository;
import com.dga.access.repository.AuthRolePermissionRepository;
import com.dga.access.repository.AuthRoleRepository;
import com.dga.access.repository.AuthUserRoleRepository;
import com.dga.access.security.CurrentUser;
import com.dga.access.service.authorization.AuthorizationCapability;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.access.service.authorization.GrantCommand;
import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthRoleService {

    @Autowired
    private AuthRoleRepository roleRepository;

    @Autowired
    private AuthRolePermissionRepository permissionRepository;

    @Autowired
    private AuthUserRoleRepository userRoleRepository;

    @Autowired
    private AuthRoleAssignmentAuditRepository auditRepository;

    @Autowired
    private AuthorizationService authorizationService;

    public List<AuthRoleView> listRoles(String cluster, String authBackend) {
        String normalizedCluster = trimToNull(cluster);
        String normalizedAuthBackend = trimToNull(authBackend);
        List<AuthRole> roles = normalizedCluster == null
                ? roleRepository.findByStatusNotOrderByCreateTimeDesc("DELETED")
                : roleRepository.findByClusterAndStatusNotOrderByCreateTimeDesc(normalizedCluster, "DELETED");
        List<AuthRoleView> result = new ArrayList<>();
        for (AuthRole role : roles) {
            if (normalizedAuthBackend != null && !equalsIgnoreCase(normalizedAuthBackend, role.getAuthBackend())) {
                continue;
            }
            result.add(view(role));
        }
        return result;
    }

    public List<BackendRoleSnapshot> listBackendRoles(BackendRoleInventoryRequest request) {
        if (request == null) {
            request = new BackendRoleInventoryRequest();
        }
        String cluster = required(request.getCluster(), "请选择集群");
        String authBackend = resolveRoleAuthBackend(cluster, request.getAuthBackend(), null);
        if (!"SENTRY".equalsIgnoreCase(authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前授权后端没有原生角色盘点能力，Ranger 角色请在 DGA 本地维护，授权时会物化为 Ranger 策略。");
        }
        request.setCluster(cluster);
        request.setAuthBackend(authBackend);
        List<BackendRoleSnapshot> snapshots = authorizationService.listBackendRoles(request);
        for (BackendRoleSnapshot snapshot : snapshots) {
            AuthRole local = roleRepository.findByRoleCode(snapshot.getRoleCode());
            snapshot.setLocalExists(local != null && !"DELETED".equalsIgnoreCase(local.getStatus()));
            snapshot.setLocalStatus(local == null ? null : local.getStatus());
        }
        return snapshots;
    }

    @Transactional
    public AuthRoleImportResult importBackendRoles(AuthRoleImportRequest request, String operator) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导入参数不能为空");
        }
        String cluster = required(request.getCluster(), "请选择集群");
        String authBackend = resolveRoleAuthBackend(cluster, request.getAuthBackend(), null);
        if (!"SENTRY".equalsIgnoreCase(authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前授权后端没有原生角色盘点能力，Ranger 角色请在 DGA 本地维护，授权时会物化为 Ranger 策略。");
        }
        if (request.getRoleCodes() == null || request.getRoleCodes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要接管的后端角色");
        }
        BackendRoleInventoryRequest inventoryRequest = new BackendRoleInventoryRequest();
        inventoryRequest.setCluster(cluster);
        inventoryRequest.setAuthBackend(authBackend);
        inventoryRequest.setRoleCodes(request.getRoleCodes());
        inventoryRequest.setIncludePermissions(request.isImportPermissions());
        inventoryRequest.setIncludeAssignments(request.isImportAssignments());

        AuthRoleImportResult result = new AuthRoleImportResult();
        List<BackendRoleSnapshot> snapshots = authorizationService.listBackendRoles(inventoryRequest);
        Map<String, BackendRoleSnapshot> snapshotByRole = new LinkedHashMap<>();
        for (BackendRoleSnapshot snapshot : snapshots) {
            snapshotByRole.put(lower(snapshot.getRoleCode()), snapshot);
        }
        result.setTotal(request.getRoleCodes().size());
        for (String requestedRoleCode : request.getRoleCodes()) {
            String roleCode = required(requestedRoleCode, "角色编码不能为空");
            BackendRoleSnapshot snapshot = snapshotByRole.get(lower(roleCode));
            if (snapshot == null) {
                addImportItem(result, roleCode, "SKIPPED", "SKIPPED", "后端未发现该角色", 0, 0);
                continue;
            }
            try {
                ImportOutcome outcome = importBackendRoleSnapshot(snapshot, cluster, authBackend, request, operator);
                addImportItem(result, snapshot.getRoleCode(), outcome.action, "SUCCESS", outcome.message,
                        outcome.permissionCount, outcome.assignmentCount);
            } catch (Exception e) {
                AuthRole failedRole = roleRepository.findByRoleCode(roleCode);
                if (failedRole != null) {
                    auditImport(failedRole, "BACKEND_ROLE_IMPORT_FAILED", null, null, null,
                            "FAILED", firstNonBlank(e.getMessage(), "后端角色接管失败"), operator);
                }
                addImportItem(result, roleCode, "FAILED", "FAILED", firstNonBlank(e.getMessage(), "后端角色接管失败"), 0, 0);
            }
        }
        return result;
    }

    private ImportOutcome importBackendRoleSnapshot(BackendRoleSnapshot snapshot, String cluster, String authBackend,
                                                    AuthRoleImportRequest request, String operator) {
        String roleCode = required(snapshot.getRoleCode(), "后端角色编码不能为空");
        AuthRole role = roleRepository.findByRoleCode(roleCode);
        boolean created = role == null || "DELETED".equalsIgnoreCase(role.getStatus());
        if (role == null) {
            role = new AuthRole();
            role.setRoleCode(roleCode);
        }
        role.setRoleName(firstNonBlank(snapshot.getRoleName(), roleCode));
        role.setCluster(cluster);
        role.setAuthBackend(authBackend);
        role.setEngineType(firstNonBlank(snapshot.getEngineType(), safeEngineType(cluster, authBackend)));
        role.setRiskLevel(firstNonBlank(role.getRiskLevel(), "LOW"));
        role.setStatus("ACTIVE");
        if (trimToNull(role.getDescription()) == null) {
            role.setDescription("从 " + authBackend + " 后端接管的已有角色");
        }
        AuthRole saved = roleRepository.save(role);

        int permissionCount = request.isImportPermissions() ? importBackendRolePermissions(saved, snapshot.getPermissions(), authBackend) : 0;
        int assignmentCount = request.isImportAssignments() ? importBackendRoleAssignments(saved, snapshot.getGroupNames(), authBackend) : 0;
        String action = created ? "CREATED" : (permissionCount == 0 && assignmentCount == 0 ? "SKIPPED" : "UPDATED");
        String message = "SKIPPED".equals(action) ? "本地角色记录已是最新" : "后端角色已接管到 DGA 本地记录";
        auditImport(saved, created ? "BACKEND_ROLE_IMPORTED" : "BACKEND_ROLE_UPDATED", null, null,
                "permissions=" + permissionCount + ", assignments=" + assignmentCount, action, message, operator);
        return new ImportOutcome(action, message, permissionCount, assignmentCount);
    }

    private int importBackendRolePermissions(AuthRole role, List<BackendRolePermissionSnapshot> permissions, String authBackend) {
        if (permissions == null || permissions.isEmpty()) {
            return 0;
        }
        int imported = 0;
        List<AuthRolePermission> existingPermissions = permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE");
        for (BackendRolePermissionSnapshot snapshot : permissions) {
            String database = trimToNull(snapshot.getDatabaseName());
            String table = trimToNull(snapshot.getTableName());
            String permission = normalizePermission(snapshot.getPermission());
            if (database == null || permission == null) {
                continue;
            }
            if (containsPermission(existingPermissions, database, table, permission, authBackend)) {
                continue;
            }
            AuthRolePermission item = new AuthRolePermission();
            item.setRoleCode(role.getRoleCode());
            item.setCluster(role.getCluster());
            item.setResourceType(table == null ? "DATABASE" : "TABLE");
            item.setDatabaseName(database);
            item.setTableName(table);
            item.setPermission(permission);
            item.setAuthBackend(authBackend);
            item.setStatus("ACTIVE");
            AuthRolePermission saved = permissionRepository.save(item);
            existingPermissions.add(saved);
            imported++;
        }
        return imported;
    }

    private int importBackendRoleAssignments(AuthRole role, List<String> groupNames, String authBackend) {
        if (groupNames == null || groupNames.isEmpty()) {
            return 0;
        }
        int imported = 0;
        for (String groupNameValue : groupNames) {
            String groupName = trimToNull(groupNameValue);
            if (groupName == null) {
                continue;
            }
            if (hasUsableAssignment(role.getRoleCode(), "GROUP", groupName, authBackend)) {
                continue;
            }
            AuthUserRole assignment = new AuthUserRole();
            assignment.setRoleCode(role.getRoleCode());
            assignment.setCluster(role.getCluster());
            assignment.setSubjectType("GROUP");
            assignment.setSubjectName(groupName);
            assignment.setAuthBackend(authBackend);
            assignment.setStatus("ACTIVE");
            assignment.setBackendSyncStatus("SUCCESS");
            assignment.setSyncMessage("从 " + authBackend + " 后端接管已有角色绑定");
            userRoleRepository.save(assignment);
            imported++;
        }
        return imported;
    }

    private boolean containsPermission(List<AuthRolePermission> permissions, String database, String table, String permission, String authBackend) {
        for (AuthRolePermission existing : permissions) {
            if (samePermission(existing, database, table, permission, authBackend)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUsableAssignment(String roleCode, String subjectType, String subjectName, String authBackend) {
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(
                roleCode, subjectType, subjectName, "ACTIVE")) {
            String backend = resolveRoleAuthBackend(assignment.getCluster(), assignment.getAuthBackend(), authBackend);
            if (equalsIgnoreCase(backend, authBackend) && isUsableAssignmentStatus(assignment.getBackendSyncStatus())) {
                return true;
            }
        }
        return false;
    }

    private void addImportItem(AuthRoleImportResult result, String roleCode, String action, String status,
                               String message, int permissionCount, int assignmentCount) {
        AuthRoleImportResult.Item item = new AuthRoleImportResult.Item();
        item.setRoleCode(roleCode);
        item.setAction(action);
        item.setStatus(status);
        item.setMessage(message);
        item.setPermissionCount(permissionCount);
        item.setAssignmentCount(assignmentCount);
        result.getItems().add(item);
        if ("FAILED".equalsIgnoreCase(status)) {
            result.setFailed(result.getFailed() + 1);
        } else if ("SKIPPED".equalsIgnoreCase(action)) {
            result.setSkipped(result.getSkipped() + 1);
        } else if ("CREATED".equalsIgnoreCase(action)) {
            result.setCreated(result.getCreated() + 1);
        } else {
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    private String normalizePermission(String permission) {
        String normalized = trimToNull(permission);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (normalized.endsWith("_PRIV")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        if ("*".equals(normalized) || "ALL PRIVILEGES".equals(normalized) || "ALL_PRIVILEGES".equals(normalized)) {
            return "ALL";
        }
        return normalized;
    }

    private void auditImport(AuthRole role, String action, String subjectType, String subjectName,
                             String resourceSummary, String backendStatus, String message, String operator) {
        AuthRoleAssignmentAudit audit = new AuthRoleAssignmentAudit();
        audit.setRoleCode(role.getRoleCode());
        audit.setCluster(role.getCluster());
        audit.setAction(action);
        audit.setSubjectType(subjectType);
        audit.setSubjectName(subjectName);
        audit.setResourceSummary(resourceSummary);
        audit.setBackendStatus(backendStatus);
        audit.setMessage(message);
        audit.setOperator(firstNonBlank(operator, CurrentUser.usernameOrUnknown()));
        auditRepository.save(audit);
    }

    private static class ImportOutcome {
        private final String action;
        private final String message;
        private final int permissionCount;
        private final int assignmentCount;

        private ImportOutcome(String action, String message, int permissionCount, int assignmentCount) {
            this.action = action;
            this.message = message;
            this.permissionCount = permissionCount;
            this.assignmentCount = assignmentCount;
        }
    }

    @Transactional
    public AuthRoleView createOrUpdateRole(AuthRoleRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色参数不能为空");
        }
        String cluster = required(request.getCluster(), "请选择角色所属集群");
        String roleCode = normalizeRoleCode(firstNonBlank(request.getRoleCode(),
                generatedRoleCode(cluster, request.getDomain(), request.getScope(), request.getLevel())));
        AuthRole role = roleRepository.findByRoleCode(roleCode);
        boolean created = role == null;
        if (role == null) {
            role = new AuthRole();
            role.setRoleCode(roleCode);
        }
        role.setRoleName(firstNonBlank(request.getRoleName(), roleCode));
        role.setCluster(cluster);
        String authBackend = resolveRoleAuthBackend(cluster, request.getAuthBackend(), role.getAuthBackend());
        role.setAuthBackend(authBackend);
        role.setEngineType(firstNonBlank(request.getEngineType(), safeEngineType(cluster, authBackend)));
        role.setOwner(request.getOwner());
        role.setRiskLevel(normalizeRisk(request.getRiskLevel()));
        role.setExpiresAt(request.getExpiresAt());
        role.setStatus(firstNonBlank(request.getStatus(), "ACTIVE").toUpperCase(Locale.ROOT));
        role.setDescription(request.getDescription());
        AuthRole saved = roleRepository.save(role);
        if (!"ACTIVE".equalsIgnoreCase(saved.getStatus())) {
            audit(saved, created ? "ROLE_CREATED_INACTIVE" : "ROLE_UPDATED", null, null, null, "SKIPPED", "角色非 ACTIVE，未下发后端");
            return view(saved);
        }
        try {
            authorizationService.ensureRole(saved.getCluster(), saved.getRoleCode(), saved.getAuthBackend());
            audit(saved, created ? "ROLE_CREATED" : "ROLE_UPDATED", null, null, null, "SUCCESS", "角色已同步到授权后端");
        } catch (Exception e) {
            audit(saved, created ? "ROLE_CREATED" : "ROLE_UPDATED", null, null, null, "FAILED", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色后端同步失败: " + e.getMessage(), e);
        }
        return view(saved);
    }

    @Transactional
    public AuthRoleView addPermission(String roleCode, AuthRolePermissionRequest request) {
        AuthRole role = activeRole(roleCode);
        if (isExpired(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色已过期，不能继续授权");
        }
        String database = required(request.getDatabaseName(), "请选择数据库");
        String permission = required(request.getPermission(), "请选择权限").toUpperCase(Locale.ROOT);
        String table = trimToNull(request.getTableName());
        if ("*".equals(database) && table != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "全部库通配仅支持库级权限，表级权限请指定具体数据库");
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), role.getAuthBackend(), null);
        if ("*".equals(database) && isStarRocksRoleBackend(role, authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "StarRocks 角色权限范围请指定具体数据库，避免后续子集授权无法展开");
        }
        for (AuthRolePermission existing : permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            if (samePermission(existing, database, table, permission, authBackend)) {
                return view(role);
            }
        }
        AuthRolePermission item = new AuthRolePermission();
        item.setRoleCode(role.getRoleCode());
        item.setCluster(role.getCluster());
        item.setResourceType(table == null ? "DATABASE" : "TABLE");
        item.setDatabaseName(database);
        item.setTableName(table);
        item.setPermission(permission);
        item.setAuthBackend(authBackend);
        item.setStatus("ACTIVE");
        AuthRolePermission saved = permissionRepository.save(item);
        try {
            authorizationService.grantPermissionToRole(role.getCluster(), role.getRoleCode(), database, table, permission, saved.getAuthBackend());
            syncPermissionToAssignments(role, saved);
            audit(role, "ROLE_PERMISSION_GRANTED", null, null, resourceSummary(saved), "SUCCESS", "角色权限已下发");
        } catch (Exception e) {
            saved.setStatus("BACKEND_FAILED");
            permissionRepository.save(saved);
            audit(role, "ROLE_PERMISSION_GRANTED", null, null, resourceSummary(saved), "FAILED", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色权限下发失败: " + e.getMessage(), e);
        }
        return view(role);
    }

    @Transactional
    public AuthRoleView deletePermission(String roleCode, Long permissionId) {
        AuthRole role = activeRole(roleCode);
        if (permissionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要删除的角色权限");
        }
        AuthRolePermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "角色权限不存在: " + permissionId));
        if (!equalsIgnoreCase(role.getRoleCode(), permission.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该权限不属于当前角色");
        }
        if (!"ACTIVE".equalsIgnoreCase(permission.getStatus())) {
            return view(role);
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), permission.getAuthBackend(), role.getAuthBackend());
        try {
            revokeRolePermissionFromBackend(role, permission, authBackend);
            permission.setStatus("DELETED");
            permissionRepository.save(permission);
            audit(role, "ROLE_PERMISSION_DELETED", null, null, resourceSummary(permission), "SUCCESS", "角色权限范围已删除");
        } catch (Exception e) {
            audit(role, "ROLE_PERMISSION_DELETED", null, null, resourceSummary(permission), "FAILED", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色权限删除失败: " + e.getMessage(), e);
        }
        return view(role);
    }

    @Transactional
    public AuthRoleView addAssignment(String roleCode, AuthRoleAssignmentRequest request) {
        AuthRole role = activeRole(roleCode);
        if (isExpired(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色已过期，不能分配");
        }
        String subjectType = required(request.getSubjectType(), "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String subjectName = required(request.getSubjectName(), "请输入绑定对象");
        if (!"USER".equals(subjectType) && !"GROUP".equals(subjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), request.getAuthBackend(), role.getAuthBackend());
        assignRoleToSubject(role, subjectType, subjectName, authBackend, request.getExpiresAt(), true);
        return view(role);
    }

    @Transactional
    public AuthUserRole addLocalOnlyAssignmentForAdoption(String roleCode, String subjectTypeValue, String subjectNameValue,
                                                         String authBackendValue, LocalDateTime expiresAt,
                                                         String operator, String message) {
        AuthRole role = activeRole(roleCode);
        if (isExpired(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色已过期，不能接管历史权限");
        }
        String subjectType = required(subjectTypeValue, "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String subjectName = required(subjectNameValue, "请输入绑定对象");
        if (!"USER".equals(subjectType) && !"GROUP".equals(subjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), authBackendValue, role.getAuthBackend());
        AuthUserRole assignment = null;
        for (AuthUserRole existing : userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(
                role.getRoleCode(), subjectType, subjectName, "ACTIVE")) {
            String existingBackend = resolveRoleAuthBackend(role.getCluster(), existing.getAuthBackend(), authBackend);
            if (!equalsIgnoreCase(existingBackend, authBackend)) {
                continue;
            }
            if (isUsableAssignmentStatus(existing.getBackendSyncStatus())) {
                return existing;
            }
            assignment = existing;
            break;
        }
        if (assignment == null) {
            assignment = new AuthUserRole();
        }
        assignment.setRoleCode(role.getRoleCode());
        assignment.setCluster(role.getCluster());
        assignment.setSubjectType(subjectType);
        assignment.setSubjectName(subjectName);
        assignment.setAuthBackend(authBackend);
        assignment.setExpiresAt(expiresAt);
        assignment.setStatus("ACTIVE");
        assignment.setBackendSyncStatus("LOCAL_ONLY");
        assignment.setSyncMessage(firstNonBlank(message, "历史权限接管：仅建立 DGA 绑定范围，未修改 Hive/Sentry 后端授权"));
        AuthUserRole saved = userRoleRepository.save(assignment);
        auditImport(role, "HISTORICAL_PERMISSION_ADOPTION_ASSIGNMENT", subjectType, subjectName,
                null, "LOCAL_ONLY", saved.getSyncMessage(), operator);
        return saved;
    }

    public boolean hasActiveAssignment(String roleCode, String subjectTypeValue, String subjectNameValue, String authBackendValue) {
        AuthRole role = activeRole(roleCode);
        String subjectType = required(subjectTypeValue, "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String subjectName = required(subjectNameValue, "请输入绑定对象");
        String authBackend = resolveRoleAuthBackend(role.getCluster(), authBackendValue, role.getAuthBackend());
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(
                role.getRoleCode(), subjectType, subjectName, "ACTIVE")) {
            String backend = resolveRoleAuthBackend(role.getCluster(), assignment.getAuthBackend(), authBackend);
            if (!equalsIgnoreCase(backend, authBackend)) {
                continue;
            }
            if (isUsableAssignmentStatus(assignment.getBackendSyncStatus())) {
                return true;
            }
        }
        return false;
    }

    public AuthRoleView effectivePermissions(String roleCode) {
        return view(activeRole(roleCode));
    }

    public BatchRoleAssignmentResult dryRunBatchAssignments(String roleCode, BatchRoleAssignmentRequest request) {
        return batchAssignments(roleCode, request, true);
    }

    @Transactional
    public BatchRoleAssignmentResult batchAssignUsers(String roleCode, BatchRoleAssignmentRequest request) {
        return batchAssignments(roleCode, request, false);
    }

    private BatchRoleAssignmentResult batchAssignments(String roleCode, BatchRoleAssignmentRequest request, boolean dryRun) {
        AuthRole role = activeRole(roleCode);
        if (isExpired(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色已过期，不能分配");
        }
        if (request == null || request.getUsernames() == null || request.getUsernames().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供用户列表");
        }
        String requestCluster = required(request.getCluster(), "请选择集群");
        if (!equalsIgnoreCase(role.getCluster(), requestCluster)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不属于当前集群");
        }
        if (request.getUsernames().size() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单次最多支持 500 个用户");
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), request.getAuthBackend(), role.getAuthBackend());
        BatchRoleAssignmentResult result = new BatchRoleAssignmentResult();
        result.setRoleCode(role.getRoleCode());
        result.setRoleName(role.getRoleName());
        result.setCluster(role.getCluster());
        result.setAuthBackend(authBackend);
        result.setDryRun(dryRun);
        result.setTotal(request.getUsernames().size());

        Map<String, AuthUserRole> existingByUser = activeAssignmentsByUser(role.getRoleCode(), authBackend);
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String input : request.getUsernames()) {
            BatchRoleAssignmentItem item = new BatchRoleAssignmentItem();
            item.setInput(input);
            String username = trimToNull(input);
            item.setUsername(username);
            if (username == null) {
                item.setAction("INVALID");
                item.setStatus("FAILED");
                item.setMessage("用户名不能为空");
                addBatchItem(result, item);
                continue;
            }
            String userKey = lower(username);
            if (seen.containsKey(userKey)) {
                item.setAction("DUPLICATE");
                item.setStatus("SKIPPED");
                item.setMessage("同批次重复用户，已跳过");
                addBatchItem(result, item);
                continue;
            }
            seen.put(userKey, Boolean.TRUE);
            AuthUserRole existing = existingByUser.get(userKey);
            if (existing != null) {
                item.setAction("ALREADY_BOUND");
                item.setStatus(firstNonBlank(existing.getBackendSyncStatus(), "SUCCESS"));
                item.setMessage(firstNonBlank(existing.getSyncMessage(), "已存在有效角色绑定，未重复创建"));
                item.setAlreadyAssigned(true);
                addBatchItem(result, item);
                continue;
            }
            if (dryRun) {
                item.setAction("WILL_BIND");
                item.setStatus(backendSupportsNativeUserRole(role.getCluster(), authBackend) ? "SUCCESS" : "PENDING_GROUP_MAPPING");
                item.setMessage(backendSupportsNativeUserRole(role.getCluster(), authBackend)
                        ? "将补充用户角色绑定并下发后端"
                        : "授权后端不支持用户直绑角色，将记录为待组映射");
                addBatchItem(result, item);
                continue;
            }
            AuthUserRole assignment = assignRoleToSubject(role, "USER", username, authBackend, request.getExpiresAt(), false);
            item.setAction("BOUND");
            item.setStatus(firstNonBlank(assignment.getBackendSyncStatus(), "SUCCESS"));
            item.setMessage(firstNonBlank(assignment.getSyncMessage(), "角色绑定已处理"));
            addBatchItem(result, item);
        }
        return result;
    }

    private Map<String, AuthUserRole> activeAssignmentsByUser(String roleCode, String authBackend) {
        Map<String, AuthUserRole> existingByUser = new LinkedHashMap<>();
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus(roleCode, "USER", "ACTIVE")) {
            String backend = resolveRoleAuthBackend(assignment.getCluster(), assignment.getAuthBackend(), authBackend);
            if (!equalsIgnoreCase(backend, authBackend)) {
                continue;
            }
            if (isUsableAssignmentStatus(assignment.getBackendSyncStatus())) {
                existingByUser.putIfAbsent(lower(assignment.getSubjectName()), assignment);
            }
        }
        return existingByUser;
    }

    private boolean isUsableAssignmentStatus(String status) {
        String normalized = firstNonBlank(status, "SUCCESS");
        return "SUCCESS".equalsIgnoreCase(normalized)
                || "PENDING_GROUP_MAPPING".equalsIgnoreCase(normalized)
                || "LOCAL_ONLY".equalsIgnoreCase(normalized);
    }

    private void addBatchItem(BatchRoleAssignmentResult result, BatchRoleAssignmentItem item) {
        result.getItems().add(item);
        String action = firstNonBlank(item.getAction(), "");
        String status = firstNonBlank(item.getStatus(), "");
        if (!"DUPLICATE".equals(action) && !"INVALID".equals(action)) {
            result.setValidCount(result.getValidCount() + 1);
        }
        if ("DUPLICATE".equals(action)) {
            result.setDuplicateCount(result.getDuplicateCount() + 1);
        }
        if ("ALREADY_BOUND".equals(action)) {
            result.setAlreadyBoundCount(result.getAlreadyBoundCount() + 1);
        }
        if ("WILL_BIND".equals(action)) {
            result.setWillBindCount(result.getWillBindCount() + 1);
        }
        if ("SUCCESS".equals(status)) {
            result.setSuccessCount(result.getSuccessCount() + 1);
        } else if ("FAILED".equals(status)) {
            result.setFailedCount(result.getFailedCount() + 1);
        } else if ("PENDING_GROUP_MAPPING".equals(status)) {
            result.setPendingGroupMappingCount(result.getPendingGroupMappingCount() + 1);
        } else if ("SKIPPED".equals(status)) {
            result.setSkippedCount(result.getSkippedCount() + 1);
        }
    }

    private AuthUserRole assignRoleToSubject(AuthRole role, String subjectType, String subjectName, String authBackend,
                                             LocalDateTime expiresAt, boolean throwOnBackendFailure) {
        AuthUserRole assignment = null;
        for (AuthUserRole existing : userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(
                role.getRoleCode(), subjectType, subjectName, "ACTIVE")) {
            String existingBackend = resolveRoleAuthBackend(role.getCluster(), existing.getAuthBackend(), authBackend);
            if (!equalsIgnoreCase(existingBackend, authBackend)) {
                continue;
            }
            String syncStatus = firstNonBlank(existing.getBackendSyncStatus(), "");
            if (isUsableAssignmentStatus(syncStatus)) {
                return existing;
            }
            assignment = existing;
            break;
        }
        if (assignment == null) {
            assignment = new AuthUserRole();
        }
        assignment.setRoleCode(role.getRoleCode());
        assignment.setCluster(role.getCluster());
        assignment.setSubjectType(subjectType);
        assignment.setSubjectName(subjectName);
        assignment.setAuthBackend(authBackend);
        assignment.setExpiresAt(expiresAt);
        assignment.setStatus("ACTIVE");
        if (isSubsetControlledGroupAssignment(role.getCluster(), assignment.getAuthBackend(), subjectType)) {
            assignment.setBackendSyncStatus("LOCAL_ONLY");
            assignment.setSyncMessage("角色绑定仅作为可授权范围，后端权限请通过子集授权下发");
            audit(role, "ROLE_ASSIGNED", subjectType, subjectName, null, "LOCAL_ONLY", assignment.getSyncMessage());
            userRoleRepository.save(assignment);
            return assignment;
        }
        try {
            if ("USER".equals(subjectType)) {
                if (backendSupportsNativeUserRole(role.getCluster(), assignment.getAuthBackend())) {
                    ensureBackendUserExists(role.getCluster(), assignment.getAuthBackend(), subjectName);
                    authorizationService.assignRoleToUser(role.getCluster(), role.getRoleCode(), subjectName, assignment.getAuthBackend());
                } else if (assignmentUsesMaterializedPolicies(role.getCluster(), assignment.getAuthBackend(), subjectType)) {
                    expandRolePermissionsToSubject(role, assignment);
                } else {
                    throw new UnsupportedOperationException("User role assignment is not supported by " + assignment.getAuthBackend());
                }
            } else {
                if (backendSupportsNativeGroupRole(role.getCluster(), assignment.getAuthBackend())) {
                    authorizationService.assignRoleToGroup(role.getCluster(), role.getRoleCode(), subjectName, assignment.getAuthBackend());
                } else if (assignmentUsesMaterializedPolicies(role.getCluster(), assignment.getAuthBackend(), subjectType)) {
                    expandRolePermissionsToSubject(role, assignment);
                } else {
                    throw new UnsupportedOperationException("Group role assignment is not supported by " + assignment.getAuthBackend());
                }
            }
            assignment.setBackendSyncStatus("SUCCESS");
            assignment.setSyncMessage("角色分配已下发");
            audit(role, "ROLE_ASSIGNED", subjectType, subjectName, null, "SUCCESS", assignment.getSyncMessage());
        } catch (UnsupportedOperationException e) {
            assignment.setBackendSyncStatus("PENDING_GROUP_MAPPING");
            assignment.setSyncMessage(e.getMessage());
            audit(role, "ROLE_ASSIGNED", subjectType, subjectName, null, "PENDING_GROUP_MAPPING", e.getMessage());
        } catch (Exception e) {
            String failureMessage = assignmentFailureMessage(role.getCluster(), assignment.getAuthBackend(), subjectType, subjectName, e);
            assignment.setBackendSyncStatus("FAILED");
            assignment.setSyncMessage(failureMessage);
            audit(role, "ROLE_ASSIGNED", subjectType, subjectName, null, "FAILED", failureMessage);
            userRoleRepository.save(assignment);
            if (throwOnBackendFailure) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色分配失败: " + failureMessage, e);
            }
            return assignment;
        }
        userRoleRepository.save(assignment);
        return assignment;
    }

    @Transactional
    public AuthRoleView revokeAssignment(String roleCode, String subjectTypeValue, String subjectNameValue, String authBackendValue) {
        AuthRole role = activeRole(roleCode);
        String subjectType = required(subjectTypeValue, "请选择绑定对象类型").toUpperCase(Locale.ROOT);
        String subjectName = required(subjectNameValue, "请输入绑定对象");
        if (!"USER".equals(subjectType) && !"GROUP".equals(subjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        String authBackend = resolveRoleAuthBackend(role.getCluster(), authBackendValue, role.getAuthBackend());
        List<AuthUserRole> assignments = userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(
                role.getRoleCode(), subjectType, subjectName, "ACTIVE");
        boolean hasBackendSuccess = false;
        for (AuthUserRole assignment : assignments) {
            String backend = resolveRoleAuthBackend(role.getCluster(), assignment.getAuthBackend(), authBackend);
            if ("SUCCESS".equalsIgnoreCase(assignment.getBackendSyncStatus())) {
                hasBackendSuccess = true;
                authBackend = backend;
            }
        }
        try {
            if (hasBackendSuccess) {
                if (assignmentUsesMaterializedPolicies(role.getCluster(), authBackend, subjectType)) {
                    revokeRolePermissionsFromSubject(role, subjectType, subjectName, authBackend);
                } else {
                    authorizationService.revokeRoleAssignment(role.getCluster(), role.getRoleCode(), subjectType, subjectName, authBackend);
                }
            }
            for (AuthUserRole assignment : assignments) {
                assignment.setStatus("REVOKED");
                assignment.setBackendSyncStatus(hasBackendSuccess ? "REVOKED" : "LOCAL_REVOKED");
                assignment.setSyncMessage(hasBackendSuccess ? "角色绑定已回收" : "本地角色绑定已回收，后端此前未成功绑定");
                userRoleRepository.save(assignment);
            }
            audit(role, "ROLE_ASSIGNMENT_REVOKED", subjectType, subjectName, null,
                    hasBackendSuccess ? "SUCCESS" : "LOCAL_ONLY",
                    hasBackendSuccess ? "角色绑定已从后端回收" : "后端此前未成功绑定，仅回收 DGA 记录");
        } catch (Exception e) {
            audit(role, "ROLE_ASSIGNMENT_REVOKED", subjectType, subjectName, null, "FAILED", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色绑定回收失败: " + e.getMessage(), e);
        }
        return view(role);
    }

    @Transactional
    public void deleteRole(String roleCode) {
        AuthRole role = activeRole(roleCode);
        role.setStatus("DELETED");
        roleRepository.save(role);
        for (AuthRolePermission permission : permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            permission.setStatus("DELETED");
            permissionRepository.save(permission);
        }
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            assignment.setStatus("DELETED");
            userRoleRepository.save(assignment);
        }
        audit(role, "ROLE_DELETED", null, null, null, "LOCAL_ONLY", "角色已从 DGA 角色库删除，未自动回收后端权限");
    }

    private void syncPermissionToAssignments(AuthRole role, AuthRolePermission permission) {
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            String backend = resolveRoleAuthBackend(role.getCluster(), assignment.getAuthBackend(), role.getAuthBackend());
            assignment.setAuthBackend(backend);
            if (!"SUCCESS".equalsIgnoreCase(assignment.getBackendSyncStatus())) {
                continue;
            }
            if (!assignmentUsesMaterializedPolicies(role.getCluster(), backend, assignment.getSubjectType())) {
                continue;
            }
            try {
                expandPermissionToSubject(role.getCluster(), backend, assignment.getSubjectType(),
                        assignment.getSubjectName(), permission);
            } catch (UnsupportedOperationException ignored) {
                // Native role backends do not need separate user/group policy expansion.
            }
        }
    }

    private void revokeRolePermissionFromBackend(AuthRole role, AuthRolePermission permission, String authBackend) {
        if (backendUsesMaterializedPolicies(role.getCluster(), authBackend)) {
            revokePermissionFromMaterializedAssignments(role, permission, authBackend);
            return;
        }
        authorizationService.revokePermissionFromRole(role.getCluster(), role.getRoleCode(),
                permission.getDatabaseName(), permission.getTableName(), permission.getPermission(), authBackend);
    }

    private void revokePermissionFromMaterializedAssignments(AuthRole role, AuthRolePermission permission, String authBackend) {
        for (AuthUserRole assignment : userRoleRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            String backend = resolveRoleAuthBackend(role.getCluster(), assignment.getAuthBackend(), authBackend);
            if (!equalsIgnoreCase(backend, authBackend)) {
                continue;
            }
            if (!isUsableAssignmentStatus(assignment.getBackendSyncStatus())) {
                continue;
            }
            if (!assignmentUsesMaterializedPolicies(role.getCluster(), backend, assignment.getSubjectType())) {
                continue;
            }
            revokePermissionFromSubject(role.getCluster(), backend, assignment.getSubjectType(), assignment.getSubjectName(), permission);
        }
    }

    private boolean samePermission(AuthRolePermission existing, String database, String table, String permission, String authBackend) {
        return equalsIgnoreCase(existing.getDatabaseName(), database)
                && equalsIgnoreCase(existing.getTableName(), table)
                && equalsIgnoreCase(existing.getPermission(), permission)
                && equalsIgnoreCase(firstNonBlank(existing.getAuthBackend(), authBackend), authBackend);
    }

    private void expandRolePermissionsToSubject(AuthRole role, AuthUserRole assignment) {
        for (AuthRolePermission permission : permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            expandPermissionToSubject(role.getCluster(), assignment.getAuthBackend(), assignment.getSubjectType(),
                    assignment.getSubjectName(), permission);
        }
    }

    private void revokeRolePermissionsFromSubject(AuthRole role, String subjectType, String subjectName, String authBackend) {
        for (AuthRolePermission permission : permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE")) {
            revokePermissionFromSubject(role.getCluster(), authBackend, subjectType, subjectName, permission);
        }
    }

    private void revokePermissionFromSubject(String cluster, String authBackend, String subjectType,
                                             String subjectName, AuthRolePermission permission) {
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            authorizationService.revokePermissionFromGroup(cluster, subjectName, permission.getDatabaseName(),
                    permission.getTableName(), permission.getPermission(), authBackend);
            return;
        }
        com.dga.access.service.authorization.RevokeCommand command = new com.dga.access.service.authorization.RevokeCommand();
        command.setCluster(cluster);
        command.setUsername(subjectName);
        command.setDatabase(permission.getDatabaseName());
        command.setTable(permission.getTableName());
        command.setPermission(permission.getPermission());
        authorizationService.revoke(command, authBackend);
    }

    private void expandPermissionToSubject(String cluster, String authBackend, String subjectType,
                                           String subjectName, AuthRolePermission permission) {
        if ("GROUP".equalsIgnoreCase(subjectType)) {
            authorizationService.grantPermissionToGroup(cluster, subjectName, permission.getDatabaseName(),
                    permission.getTableName(), permission.getPermission(), authBackend);
            return;
        }
        GrantCommand command = new GrantCommand();
        command.setCluster(cluster);
        command.setUsername(subjectName);
        command.setDatabase(permission.getDatabaseName());
        command.setTable(permission.getTableName());
        command.setPermission(permission.getPermission());
        authorizationService.grant(command, authBackend);
    }

    private AuthRole activeRole(String roleCode) {
        AuthRole role = roleRepository.findByRoleCode(roleCode);
        if (role == null || "DELETED".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "角色不存在: " + roleCode);
        }
        if (!"ACTIVE".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不是 ACTIVE 状态: " + roleCode);
        }
        return role;
    }

    private AuthRoleView view(AuthRole role) {
        List<AuthRolePermission> permissions = dedupePermissions(permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE"));
        List<AuthUserRole> assignments = dedupeAssignments(userRoleRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE"));
        String authBackend = resolveViewAuthBackend(role, permissions, assignments);
        role.setAuthBackend(firstNonBlank(role.getAuthBackend(), authBackend));
        role.setEngineType(firstNonBlank(role.getEngineType(), safeEngineType(role.getCluster(), authBackend)));
        for (AuthRolePermission permission : permissions) {
            permission.setAuthBackend(firstNonBlank(permission.getAuthBackend(), authBackend));
        }
        for (AuthUserRole assignment : assignments) {
            assignment.setAuthBackend(firstNonBlank(assignment.getAuthBackend(), authBackend));
        }
        return new AuthRoleView(role,
                permissions,
                assignments);
    }

    private List<AuthRolePermission> dedupePermissions(List<AuthRolePermission> permissions) {
        Map<String, AuthRolePermission> unique = new LinkedHashMap<>();
        for (AuthRolePermission permission : permissions) {
            unique.putIfAbsent(permissionKey(permission), permission);
        }
        return new ArrayList<>(unique.values());
    }

    private List<AuthUserRole> dedupeAssignments(List<AuthUserRole> assignments) {
        Map<String, AuthUserRole> unique = new LinkedHashMap<>();
        for (AuthUserRole assignment : assignments) {
            unique.putIfAbsent(assignmentKey(assignment), assignment);
        }
        return new ArrayList<>(unique.values());
    }

    private String permissionKey(AuthRolePermission permission) {
        return String.join("|",
                upper(permission.getResourceType()),
                lower(permission.getDatabaseName()),
                lower(firstNonBlank(permission.getTableName(), "*")),
                upper(permission.getPermission()),
                upper(permission.getAuthBackend()));
    }

    private String assignmentKey(AuthUserRole assignment) {
        return String.join("|",
                upper(assignment.getSubjectType()),
                lower(assignment.getSubjectName()),
                upper(assignment.getAuthBackend()),
                upper(assignment.getBackendSyncStatus()));
    }

    private String resolveViewAuthBackend(AuthRole role, List<AuthRolePermission> permissions, List<AuthUserRole> assignments) {
        String backend = authorizationService.normalizeAuthBackend(role.getAuthBackend());
        if (backend != null) {
            return backend;
        }
        for (AuthRolePermission permission : permissions) {
            backend = authorizationService.normalizeAuthBackend(permission.getAuthBackend());
            if (backend != null) {
                return backend;
            }
        }
        for (AuthUserRole assignment : assignments) {
            backend = authorizationService.normalizeAuthBackend(assignment.getAuthBackend());
            if (backend != null) {
                return backend;
            }
        }
        return authorizationService.authBackend(role.getCluster());
    }

    private void audit(AuthRole role, String action, String subjectType, String subjectName,
                       String resourceSummary, String backendStatus, String message) {
        AuthRoleAssignmentAudit audit = new AuthRoleAssignmentAudit();
        audit.setRoleCode(role.getRoleCode());
        audit.setCluster(role.getCluster());
        audit.setAction(action);
        audit.setSubjectType(subjectType);
        audit.setSubjectName(subjectName);
        audit.setResourceSummary(resourceSummary);
        audit.setBackendStatus(backendStatus);
        audit.setMessage(message);
        audit.setOperator(CurrentUser.usernameOrUnknown());
        auditRepository.save(audit);
    }

    private String generatedRoleCode(String cluster, String domain, String scope, String level) {
        String base = "dga_" + firstNonBlank(cluster, "cluster") + "_" + firstNonBlank(domain, "data")
                + "_" + firstNonBlank(scope, "resource") + "_" + firstNonBlank(level, "readonly");
        return normalizeRoleCode(base);
    }

    private String normalizeRoleCode(String value) {
        String normalized = required(value, "roleCode 不能为空").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (!normalized.startsWith("dga_")) {
            normalized = "dga_" + normalized;
        }
        if (normalized.length() <= 96) {
            return normalized;
        }
        return normalized.substring(0, 87) + "_" + shortHash(normalized);
    }

    private String shortHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf(Math.abs(text.hashCode()));
        }
    }

    private boolean isExpired(AuthRole role) {
        return role.getExpiresAt() != null && role.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private String normalizeRisk(String value) {
        return firstNonBlank(value, "LOW").toUpperCase(Locale.ROOT);
    }

    private String resolveRoleAuthBackend(String cluster, String preferredAuthBackend, String fallbackAuthBackend) {
        String backend = authorizationService.normalizeAuthBackend(firstNonBlank(preferredAuthBackend, fallbackAuthBackend));
        return backend != null ? backend : authorizationService.authBackend(cluster);
    }

    private boolean isStarRocksRoleBackend(AuthRole role, String authBackend) {
        if (ClusterEndpoint.AUTH_STARROCKS_SQL.equalsIgnoreCase(trimToNull(authBackend))) {
            return true;
        }
        String engineType = upper(role.getEngineType());
        if (engineType.contains("STARROCKS") || engineType.contains("STAR_ROCKS")) {
            return true;
        }
        return upper(safeEngineType(role.getCluster(), authBackend)).contains("STARROCKS");
    }

    private boolean backendUsesMaterializedPolicies(String cluster, String authBackend) {
        return capabilityFor(cluster, authBackend).getRbac().isUsesMaterializedPolicies();
    }

    private boolean assignmentUsesMaterializedPolicies(String cluster, String authBackend, String subjectType) {
        AuthorizationCapability capability = capabilityFor(cluster, authBackend);
        if (backendUsesMaterializedPolicies(cluster, authBackend)) {
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

    private boolean isSubsetControlledGroupAssignment(String cluster, String authBackend, String subjectType) {
        if (!"GROUP".equalsIgnoreCase(subjectType)) {
            return false;
        }
        AuthorizationCapability capability = capabilityFor(cluster, authBackend);
        return capability != null
                && capability.getGrant() != null
                && capability.getGrant().isSupportsRoleSubsetGrant()
                && capability.getGrant().isSupportsGroupRoleSubsetGrant();
    }

    private boolean backendSupportsNativeGroupRole(String cluster, String authBackend) {
        return capabilityFor(cluster, authBackend).getRbac().isSupportsNativeGroupRole();
    }

    private boolean backendSupportsNativeUserRole(String cluster, String authBackend) {
        return capabilityFor(cluster, authBackend).getRbac().isSupportsNativeUserRole();
    }

    private boolean backendRequiresExistingBackendUser(String cluster, String authBackend) {
        return authorizationService.requiresExistingBackendUser(cluster, authBackend);
    }

    private void ensureBackendUserExists(String cluster, String authBackend, String username) {
        if (!backendRequiresExistingBackendUser(cluster, authBackend)) {
            return;
        }
        if (authorizationService.userExists(cluster, username, authBackend)) {
            return;
        }
        throw new IllegalArgumentException(missingBackendUserMessage(authBackend, username));
    }

    private String assignmentFailureMessage(String cluster, String authBackend, String subjectType, String subjectName, Exception e) {
        if ("USER".equalsIgnoreCase(subjectType)
                && backendRequiresExistingBackendUser(cluster, authBackend)
                && isMissingBackendUserError(e)) {
            return missingBackendUserMessage(authBackend, subjectName);
        }
        if (e instanceof ResponseStatusException) {
            String reason = ((ResponseStatusException) e).getReason();
            if (reason != null && !reason.trim().isEmpty()) {
                return reason.trim();
            }
        }
        return firstNonBlank(e.getMessage(), "角色分配失败");
    }

    private String missingBackendUserMessage(String authBackend, String username) {
        return firstNonBlank(authBackend, "授权后端") + " 用户不存在: " + username + "，请先创建/导入该用户后再绑定角色";
    }

    private boolean isMissingBackendUserError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("cannot find user")
                        || lower.contains("unknown user")
                        || lower.contains("no such user")
                        || lower.contains("doesn't exist")
                        || lower.contains("does not exist")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private AuthorizationCapability capabilityFor(String cluster, String authBackend) {
        return authorizationService.capability(cluster, authBackend);
    }

    private String safeEngineType(String cluster, String authBackend) {
        try {
            return authorizationService.engineType(cluster, authBackend);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String resourceSummary(AuthRolePermission permission) {
        return permission.getPermission() + " " + permission.getDatabaseName()
                + (permission.getTableName() == null ? "" : "." + permission.getTableName());
    }

    private String required(String value, String message) {
        String text = trimToNull(value);
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String text = trimToNull(value);
            if (text != null) return text;
        }
        return null;
    }

    private String lower(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private String upper(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text.toUpperCase(Locale.ROOT);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        String normalizedLeft = trimToNull(left);
        String normalizedRight = trimToNull(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return normalizedLeft == null && normalizedRight == null;
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }
}
