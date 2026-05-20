package com.dga.access.service;

import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionPreview;
import com.dga.access.dto.HistoricalPermissionAdoptionPreviewRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionResult;
import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.AuthRolePermissionRepository;
import com.dga.access.repository.AuthRoleRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.service.authorization.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HistoricalPermissionAdoptionService {

    private static final String GRANT_MODE = "ROLE_ADOPTION";

    @Autowired
    private AuthRoleRepository roleRepository;

    @Autowired
    private AuthRolePermissionRepository permissionRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private AuthRoleService authRoleService;

    public HistoricalPermissionAdoptionPreview preview(String roleCode, HistoricalPermissionAdoptionPreviewRequest request) {
        PreviewContext context = buildPreviewContext(roleCode, request);
        HistoricalPermissionAdoptionPreview preview = buildPreview(context);
        preview.setSnapshotHash(snapshotHash(preview));
        return preview;
    }

    @Transactional
    public HistoricalPermissionAdoptionResult adopt(String roleCode, HistoricalPermissionAdoptionRequest request, String operator) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "接管参数不能为空");
        }
        HistoricalPermissionAdoptionPreviewRequest previewRequest = new HistoricalPermissionAdoptionPreviewRequest();
        previewRequest.setUsername(request.getUsername());
        previewRequest.setCluster(request.getCluster());
        previewRequest.setAuthBackend(request.getAuthBackend());
        previewRequest.setSubjectType(request.getSubjectType());
        previewRequest.setSubjectName(request.getSubjectName());
        previewRequest.setIncludeGroupInherited(request.getAdoptGroupInheritedPermissions());
        PreviewContext context = buildPreviewContext(roleCode, previewRequest);
        HistoricalPermissionAdoptionPreview preview = buildPreview(context);
        preview.setSnapshotHash(snapshotHash(preview));
        if (trimToNull(request.getExpectedSnapshotHash()) != null
                && !request.getExpectedSnapshotHash().equals(preview.getSnapshotHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "后端 live 权限已变化，请刷新接管预览后重试");
        }

        Map<String, HistoricalPermissionAdoptionPreview.Item> previewItems = new LinkedHashMap<>();
        for (HistoricalPermissionAdoptionPreview.Item item : preview.getItems()) {
            previewItems.put(item.getKey(), item);
        }
        Set<String> selectedKeys = selectedKeys(request.getRolePermissions(), context.authBackend);
        if (selectedKeys.isEmpty()) {
            for (HistoricalPermissionAdoptionPreview.Item item : preview.getItems()) {
                if (item.isAdoptable()) {
                    selectedKeys.add(item.getKey());
                }
            }
        }
        if (selectedKeys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前角色与历史权限没有可接管的权限项");
        }
        validateAcknowledgements(preview, selectedKeys, previewItems, request);

        HistoricalPermissionAdoptionResult result = new HistoricalPermissionAdoptionResult();
        result.setUsername(context.username);
        result.setCluster(context.cluster);
        result.setAuthBackend(context.authBackend);
        result.setRoleCode(context.role.getRoleCode());
        result.setSubjectType(context.subjectType);
        result.setSubjectName(context.subjectName);
        result.setReconciliationStatus(preview.getReconciliationStatus());
        result.setRequestedCount(selectedKeys.size());
        result.setWarnings(preview.getWarnings());

        Map<String, UserResourceAccess> existingAdoptions = existingAdoptionRecords(context);
        Set<String> recordedKeys = context.recordedByKey.keySet();
        Set<String> assignmentKeys = new HashSet<>();
        for (String key : selectedKeys) {
            HistoricalPermissionAdoptionPreview.Item item = previewItems.get(key);
            if (item == null) {
                addResultItem(result, key, "BLOCKED", "所选权限不在当前预览结果中", null, null, null, null);
                continue;
            }
            if (existingAdoptions.containsKey(key) || recordedKeys.contains(key) || "ALREADY_RECORDED".equals(item.getAdoptionStatus())) {
                UserResourceAccess existing = existingAdoptions.get(key);
                addResultItem(result, key, "SKIPPED_EXISTING", "DGA 已有该权限记录，未重复接管",
                        existing == null ? null : existing.getId(), item.getSource(), item.getSourceRole(), item.getSourceGroup());
                continue;
            }
            if (!canPersist(item, request)) {
                addResultItem(result, key, "BLOCKED", blockedMessage(item), null, item.getSource(), item.getSourceRole(), item.getSourceGroup());
                continue;
            }
            String assignmentSubjectType = context.subjectType;
            String assignmentSubjectName = context.subjectName;
            if (isGroupInherited(item)) {
                assignmentSubjectType = "GROUP";
                assignmentSubjectName = requireText(item.getSourceGroup(), "LDAP 组继承权限缺少来源组，不能接管");
            }
            String assignmentKey = assignmentSubjectType + "|" + assignmentSubjectName.toLowerCase(Locale.ROOT);
            if (Boolean.TRUE.equals(request.getBindRoleLocalOnly()) || request.getBindRoleLocalOnly() == null) {
                if (assignmentKeys.add(assignmentKey)) {
                    authRoleService.addLocalOnlyAssignmentForAdoption(context.role.getRoleCode(), assignmentSubjectType,
                            assignmentSubjectName, context.authBackend, null, operator,
                            "历史权限接管：仅建立 DGA 绑定范围，未修改 Hive/Sentry 后端授权");
                    result.setAssignmentStatus("LOCAL_ONLY");
                }
            }
            UserResourceAccess saved = saveAdoptionRecord(context, item, assignmentSubjectType, assignmentSubjectName, request, operator);
            addResultItem(result, key, "ADOPTED", "历史权限已接管为 DGA 本地记录", saved.getId(),
                    item.getSource(), item.getSourceRole(), item.getSourceGroup());
        }
        return result;
    }

    private PreviewContext buildPreviewContext(String roleCodeValue, HistoricalPermissionAdoptionPreviewRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预览参数不能为空");
        }
        String roleCode = requireText(roleCodeValue, "请选择角色");
        AuthRole role = roleRepository.findByRoleCode(roleCode);
        if (role == null || "DELETED".equalsIgnoreCase(role.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在或已删除: " + roleCode);
        }
        String cluster = firstNonBlank(request.getCluster(), role.getCluster());
        if (!equalsIgnoreCase(role.getCluster(), cluster)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不属于当前集群");
        }
        String authBackend = authorizationService.normalizeAuthBackend(firstNonBlank(request.getAuthBackend(), role.getAuthBackend()));
        if (authBackend == null) {
            authBackend = authorizationService.authBackend(cluster);
        }
        if (!"SENTRY".equalsIgnoreCase(authBackend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "历史权限接管当前仅支持 Hive + Sentry");
        }
        String username = requireText(request.getUsername(), "请选择历史用户");
        String subjectType = firstNonBlank(request.getSubjectType(), "USER").toUpperCase(Locale.ROOT);
        if (!"USER".equals(subjectType) && !"GROUP".equals(subjectType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "绑定对象类型仅支持 USER 或 GROUP");
        }
        String subjectName = firstNonBlank(request.getSubjectName(), "USER".equals(subjectType) ? username : null);
        if (subjectName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入绑定对象");
        }
        String resolvedCluster = authorizationService.resolveClusterCodeOrName(cluster);
        PreviewContext context = new PreviewContext();
        context.role = role;
        context.cluster = cluster;
        context.resolvedCluster = resolvedCluster;
        context.authBackend = authBackend;
        context.username = username;
        context.subjectType = subjectType;
        context.subjectName = subjectName;
        context.includeGroupInherited = Boolean.TRUE.equals(request.getIncludeGroupInherited());
        context.rolePermissions = permissionRepository.findByRoleCodeAndStatus(role.getRoleCode(), "ACTIVE");
        context.livePermissions = authorizationService.getUserPermissions(username, cluster, authBackend);
        context.recordedAccess = resolvedCluster == null || resolvedCluster.trim().isEmpty()
                ? userResourceAccessRepository.findByUsernameAndIsDeletedFalse(username)
                : userResourceAccessRepository.findByUsernameAndClusterCodeAndIsDeletedFalse(username, resolvedCluster);
        return context;
    }

    private HistoricalPermissionAdoptionPreview buildPreview(PreviewContext context) {
        Map<String, PermissionRecord> roleByKey = rolePermissionsByKey(context.rolePermissions, context.authBackend);
        Map<String, PermissionRecord> directByKey = new LinkedHashMap<>();
        Map<String, PermissionRecord> groupByKey = new LinkedHashMap<>();
        for (Map<String, Object> row : context.livePermissions) {
            PermissionRecord record = livePermission(row, context.authBackend);
            if (record.key == null || record.databaseName == null) {
                continue;
            }
            if ("GROUP_ROLE".equals(record.source)) {
                groupByKey.putIfAbsent(record.key, record);
            } else if ("USER".equals(record.source) || "USER_ROLE".equals(record.source) || record.source == null) {
                if (record.source == null) {
                    record.source = "USER";
                }
                directByKey.putIfAbsent(record.key, record);
            }
        }
        Map<String, PermissionRecord> recordedByKey = new LinkedHashMap<>();
        for (UserResourceAccess access : context.recordedAccess) {
            PermissionRecord record = recordedPermission(access, context.authBackend);
            if (record.key != null) {
                recordedByKey.putIfAbsent(record.key, record);
            }
        }
        context.recordedByKey = recordedByKey;

        HistoricalPermissionAdoptionPreview preview = new HistoricalPermissionAdoptionPreview();
        preview.setUsername(context.username);
        preview.setCluster(context.cluster);
        preview.setAuthBackend(context.authBackend);
        preview.setRoleCode(context.role.getRoleCode());
        preview.setSubjectType(context.subjectType);
        preview.setSubjectName(context.subjectName);

        Set<String> roleKeys = roleByKey.keySet();
        Set<String> directKeys = directByKey.keySet();
        Set<String> groupKeys = groupByKey.keySet();
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(roleKeys);
        allKeys.addAll(directKeys);
        allKeys.addAll(groupKeys);
        allKeys.addAll(recordedByKey.keySet());
        List<String> sortedKeys = new ArrayList<>(allKeys);
        Collections.sort(sortedKeys);

        HistoricalPermissionAdoptionPreview.Summary summary = new HistoricalPermissionAdoptionPreview.Summary();
        summary.setRolePermissionCount(roleKeys.size());
        for (PermissionRecord record : directByKey.values()) {
            if ("USER_ROLE".equals(record.source)) {
                summary.setLiveUserRoleCount(summary.getLiveUserRoleCount() + 1);
            } else {
                summary.setLiveDirectCount(summary.getLiveDirectCount() + 1);
            }
        }
        summary.setLiveGroupInheritedCount(groupKeys.size());
        summary.setRecordedCount(recordedByKey.size());

        for (String key : sortedKeys) {
            PermissionRecord base = firstRecord(roleByKey.get(key), directByKey.get(key), groupByKey.get(key), recordedByKey.get(key));
            HistoricalPermissionAdoptionPreview.Item item = previewItem(base);
            boolean inRole = roleByKey.containsKey(key);
            boolean inDirect = directByKey.containsKey(key);
            boolean inGroup = groupByKey.containsKey(key);
            boolean inRecorded = recordedByKey.containsKey(key);
            PermissionRecord live = firstRecord(directByKey.get(key), groupByKey.get(key));
            PermissionRecord recorded = recordedByKey.get(key);
            if (live != null) {
                item.setSource(live.source);
                item.setSourceRole(live.sourceRole);
                item.setSourceGroup(live.sourceGroup);
                item.setGrantText(live.grantText);
            } else if (recorded != null) {
                item.setSource(recorded.source);
            }
            item.setVerificationStatus(live != null && recorded != null ? "MATCHED" : live != null ? "LIVE_ONLY" : recorded != null ? "RECORDED_ONLY" : "ROLE_ONLY");
            if (inRecorded && inRole) {
                item.setAdoptionStatus("ALREADY_RECORDED");
                item.setWarning("DGA 已有该权限记录，重复接管会跳过");
                summary.setAlreadyRecordedCount(summary.getAlreadyRecordedCount() + 1);
            } else if (inRole && inDirect) {
                item.setAdoptionStatus("DIRECT_MATCH");
                item.setAdoptable(true);
                summary.setMatchedDirectCount(summary.getMatchedDirectCount() + 1);
                summary.setAdoptableCount(summary.getAdoptableCount() + 1);
            } else if (inRole && inGroup) {
                item.setAdoptionStatus("GROUP_INHERITED_MATCH");
                item.setRequiresGroupInheritedAcknowledgement(true);
                item.setAdoptable(context.includeGroupInherited);
                item.setWarning("该权限来自 LDAP 组继承，不能作为用户私有权限接管");
                summary.setMatchedGroupInheritedCount(summary.getMatchedGroupInheritedCount() + 1);
                if (context.includeGroupInherited) {
                    summary.setAdoptableCount(summary.getAdoptableCount() + 1);
                } else {
                    summary.setBlockedGroupInheritedCount(summary.getBlockedGroupInheritedCount() + 1);
                }
            } else if (inRole) {
                item.setAdoptionStatus("ROLE_MISSING_LIVE");
                item.setWarning("角色包含该权限，但历史用户当前 live 权限中不存在；接管不会自动补发");
                summary.setRoleMissingLiveCount(summary.getRoleMissingLiveCount() + 1);
            } else if (inDirect) {
                item.setAdoptionStatus("LIVE_EXTRA_DIRECT");
                item.setWarning("历史用户拥有该权限，但不在当前角色范围内；不会塞入该角色");
                summary.setLiveExtraDirectCount(summary.getLiveExtraDirectCount() + 1);
            } else if (inGroup) {
                item.setAdoptionStatus("LIVE_EXTRA_GROUP_INHERITED");
                item.setWarning("该 LDAP 组继承权限不在当前角色范围内");
                summary.setLiveExtraGroupInheritedCount(summary.getLiveExtraGroupInheritedCount() + 1);
            } else if (inRecorded) {
                item.setAdoptionStatus("RECORDED_ONLY");
                item.setWarning("DGA 有记录但 live 权限中不存在");
            }
            preview.getItems().add(item);
        }
        preview.setSummary(summary);
        preview.setReconciliationStatus(reconciliationStatus(roleKeys, directKeys, groupKeys, recordedByKey.keySet()));
        addWarnings(preview);
        return preview;
    }

    private void validateAcknowledgements(HistoricalPermissionAdoptionPreview preview, Set<String> selectedKeys,
                                          Map<String, HistoricalPermissionAdoptionPreview.Item> previewItems,
                                          HistoricalPermissionAdoptionRequest request) {
        String status = preview.getReconciliationStatus();
        if ("NO_OVERLAP".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前角色与历史权限无可接管交集");
        }
        if (preview.getSummary().getRoleMissingLiveCount() > 0 && !Boolean.TRUE.equals(request.getAllowRoleMissingBackendPermissions())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色范围中存在历史用户未拥有的权限，请确认不会自动补发缺失权限");
        }
        if (preview.getSummary().getLiveExtraDirectCount() > 0 && !Boolean.TRUE.equals(request.getAllowSupersetExtrasUnmanaged())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "历史用户存在角色外权限，请确认这些权限将继续保持未接管状态");
        }
        if ("PARTIAL_OVERLAP".equals(status) && !Boolean.TRUE.equals(request.getAllowPartialAdoption())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "历史权限与角色范围部分重叠，请确认只接管重叠部分");
        }
        for (String key : selectedKeys) {
            HistoricalPermissionAdoptionPreview.Item item = previewItems.get(key);
            if (item != null && isGroupInherited(item)
                    && (!Boolean.TRUE.equals(request.getAdoptGroupInheritedPermissions())
                    || !Boolean.TRUE.equals(request.getAcknowledgeGroupInherited()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选权限包含 LDAP 组继承权限，请确认按组来源接管且不作为用户私有权限");
            }
        }
    }

    private UserResourceAccess saveAdoptionRecord(PreviewContext context, HistoricalPermissionAdoptionPreview.Item item,
                                                  String subjectType, String subjectName,
                                                  HistoricalPermissionAdoptionRequest request, String operator) {
        UserResourceAccess access = new UserResourceAccess();
        access.setUsername(context.username);
        access.setClusterName(context.cluster);
        access.setClusterCode(context.resolvedCluster);
        access.setEngineType(authorizationService.engineType(context.cluster, context.authBackend));
        access.setAuthBackend(context.authBackend);
        access.setResourceType(firstNonBlank(item.getResourceType(), item.getTableName() == null ? "DATABASE" : "TABLE"));
        access.setDatabaseName(item.getDatabaseName());
        access.setTableName(normalizeTable(item.getTableName()));
        access.setPermission(normalizePermission(item.getPermission()));
        access.setGrantMode(GRANT_MODE);
        access.setRoleCode(context.role.getRoleCode());
        access.setSubjectType(subjectType);
        access.setSubjectName(subjectName);
        access.setExceptionReason(firstNonBlank(request.getAdoptionReason(), "历史权限接管：未修改 Hive/Sentry 后端授权"));
        access.setTicketNo(request.getTicketNo());
        access.setApprover(request.getApprover());
        access.setRiskLevel(riskLevelFor(item.getPermission()));
        access.setGrantedBy(operator);
        access.setSource(firstNonBlank(item.getSource(), "USER"));
        access.setStatus("ACTIVE");
        access.setDeleted(false);
        access.setGrantTime(LocalDateTime.now());
        return userResourceAccessRepository.save(access);
    }

    private Map<String, UserResourceAccess> existingAdoptionRecords(PreviewContext context) {
        Map<String, UserResourceAccess> result = new LinkedHashMap<>();
        for (UserResourceAccess access : userResourceAccessRepository.findActiveRoleAdoptionRecords(
                context.username, context.resolvedCluster, context.authBackend, context.role.getRoleCode())) {
            PermissionRecord record = recordedPermission(access, context.authBackend);
            if (record.key != null) {
                result.putIfAbsent(record.key, access);
            }
        }
        return result;
    }

    private Map<String, PermissionRecord> rolePermissionsByKey(List<AuthRolePermission> permissions, String fallbackAuthBackend) {
        Map<String, PermissionRecord> result = new LinkedHashMap<>();
        for (AuthRolePermission permission : permissions == null ? Collections.<AuthRolePermission>emptyList() : permissions) {
            PermissionRecord record = rolePermission(permission, fallbackAuthBackend);
            if (record.key != null && record.databaseName != null) {
                result.putIfAbsent(record.key, record);
            }
        }
        return result;
    }

    private PermissionRecord rolePermission(AuthRolePermission permission, String fallbackAuthBackend) {
        PermissionRecord record = new PermissionRecord();
        record.resourceType = normalizedResourceType(permission.getResourceType(), permission.getTableName());
        record.databaseName = trimToNull(permission.getDatabaseName());
        record.tableName = normalizeTable(permission.getTableName());
        record.permission = normalizePermission(permission.getPermission());
        record.authBackend = normalizeAuthBackend(firstNonBlank(permission.getAuthBackend(), fallbackAuthBackend));
        record.key = key(record);
        return record;
    }

    private PermissionRecord livePermission(Map<String, Object> row, String fallbackAuthBackend) {
        Map<String, Object> values = lowerCaseKeys(row);
        PermissionRecord record = new PermissionRecord();
        String grantText = firstNonBlank(
                stringValue(values.get("granttext")),
                stringValue(values.get("grants")),
                stringValue(values.get("grant")),
                stringValue(values.get("grant_stmt")),
                stringValue(values.get("grant_statement")),
                stringValue(values.get("rawgrant")),
                stringValue(values.get("raw")));
        String database = firstNonBlank(
                stringValue(values.get("databasename")),
                stringValue(values.get("database")),
                stringValue(values.get("database_name")),
                stringValue(values.get("db")),
                stringValue(values.get("db_name")));
        String table = firstNonBlank(
                stringValue(values.get("tablename")),
                stringValue(values.get("table")),
                stringValue(values.get("table_name")),
                stringValue(values.get("tbl")));
        String permission = firstNonBlank(
                stringValue(values.get("permission")),
                stringValue(values.get("action")),
                stringValue(values.get("privilege")));
        if (grantText == null && permission != null && permission.toUpperCase(Locale.ROOT).startsWith("GRANT ")) {
            grantText = permission;
        }
        String resourceType = stringValue(values.get("resourcetype"));
        if (resourceType == null) {
            resourceType = stringValue(values.get("resource_type"));
        }
        if (grantText != null && grantText.toUpperCase(Locale.ROOT).startsWith("GRANT ")) {
            Map<String, String> parsed = parseGrantText(grantText);
            database = firstNonBlank(database, parsed.get("database"));
            table = firstNonBlank(table, parsed.get("table"));
            if (permission == null || permission.toUpperCase(Locale.ROOT).startsWith("GRANT ")) {
                permission = parsed.get("permission");
            }
            resourceType = firstNonBlank(resourceType, parsed.get("resourceType"));
        }
        record.databaseName = database;
        record.tableName = normalizeTable(table);
        record.resourceType = normalizedResourceType(resourceType, record.tableName);
        record.permission = normalizePermission(permission);
        record.authBackend = normalizeAuthBackend(firstNonBlank(stringValue(values.get("authbackend")), stringValue(values.get("auth_backend")), fallbackAuthBackend));
        record.source = normalizeSource(stringValue(values.get("source")));
        record.sourceRole = firstNonBlank(stringValue(values.get("sourcerole")), stringValue(values.get("source_role")));
        record.sourceGroup = firstNonBlank(stringValue(values.get("sourcegroup")), stringValue(values.get("source_group")));
        record.grantText = grantText;
        record.key = key(record);
        return record;
    }

    private PermissionRecord recordedPermission(UserResourceAccess access, String fallbackAuthBackend) {
        PermissionRecord record = new PermissionRecord();
        record.resourceType = normalizedResourceType(access.getResourceType(), access.getTableName());
        record.databaseName = trimToNull(access.getDatabaseName());
        record.tableName = normalizeTable(access.getTableName());
        record.permission = normalizePermission(access.getPermission());
        record.authBackend = normalizeAuthBackend(firstNonBlank(access.getAuthBackend(), fallbackAuthBackend));
        record.source = normalizeSource(access.getSource());
        record.key = key(record);
        return record;
    }

    private HistoricalPermissionAdoptionPreview.Item previewItem(PermissionRecord record) {
        HistoricalPermissionAdoptionPreview.Item item = new HistoricalPermissionAdoptionPreview.Item();
        item.setKey(record.key);
        item.setResourceType(record.resourceType);
        item.setDatabaseName(record.databaseName);
        item.setTableName(record.tableName);
        item.setPermission(record.permission);
        item.setAuthBackend(record.authBackend);
        return item;
    }

    private Map<String, String> parseGrantText(String grantText) {
        Map<String, String> parsed = new HashMap<>();
        Matcher privilegeMatcher = Pattern.compile("(?i)^GRANT\\s+(.+?)\\s+ON\\s+").matcher(grantText);
        if (privilegeMatcher.find()) {
            parsed.put("permission", privilegeMatcher.group(1).trim().replace("_PRIV", ""));
        }
        if (Pattern.compile("(?i)ON\\s+SERVER\\s+").matcher(grantText).find()
                || Pattern.compile("(?i)ON\\s+ALL\\s+TABLES\\s+IN\\s+ALL\\s+DATABASES").matcher(grantText).find()) {
            parsed.put("resourceType", "DATABASE");
            parsed.put("database", "*");
            return parsed;
        }
        Matcher allTablesMatcher = Pattern.compile("(?i)ON\\s+ALL\\s+TABLES\\s+IN\\s+DATABASE\\s+`?([^`\\s]+)`?").matcher(grantText);
        if (allTablesMatcher.find()) {
            parsed.put("resourceType", "DATABASE");
            parsed.put("database", allTablesMatcher.group(1));
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

    private String reconciliationStatus(Set<String> roleKeys, Set<String> directKeys, Set<String> groupKeys, Set<String> recordedKeys) {
        if (!roleKeys.isEmpty() && recordedKeys.containsAll(roleKeys)) {
            return "ALREADY_ADOPTED";
        }
        Set<String> directOverlap = intersection(roleKeys, directKeys);
        Set<String> groupOverlap = intersection(roleKeys, groupKeys);
        if (!roleKeys.isEmpty() && roleKeys.equals(directKeys)) {
            return "EXACT_MATCH";
        }
        if (directOverlap.isEmpty() && !groupOverlap.isEmpty()) {
            return "GROUP_INHERITED_ONLY";
        }
        if (!directOverlap.isEmpty() && roleKeys.containsAll(directKeys) && !roleKeys.equals(directKeys)) {
            return "SUBSET_OF_ROLE";
        }
        if (!directOverlap.isEmpty() && directKeys.containsAll(roleKeys) && !roleKeys.equals(directKeys)) {
            return "SUPERSET_OF_ROLE";
        }
        if (!directOverlap.isEmpty()) {
            return "PARTIAL_OVERLAP";
        }
        return "NO_OVERLAP";
    }

    private Set<String> selectedKeys(List<BatchGrantRequest.RolePermissionSelection> selections, String fallbackAuthBackend) {
        Set<String> result = new LinkedHashSet<>();
        if (selections == null) {
            return result;
        }
        for (BatchGrantRequest.RolePermissionSelection selection : selections) {
            PermissionRecord record = new PermissionRecord();
            record.resourceType = normalizedResourceType(selection.getResourceType(), selection.getTableName());
            record.databaseName = trimToNull(selection.getDatabaseName());
            record.tableName = normalizeTable(selection.getTableName());
            record.permission = normalizePermission(selection.getPermission());
            record.authBackend = normalizeAuthBackend(firstNonBlank(selection.getAuthBackend(), fallbackAuthBackend));
            String key = key(record);
            if (key != null) {
                result.add(key);
            }
        }
        return result;
    }

    private boolean canPersist(HistoricalPermissionAdoptionPreview.Item item, HistoricalPermissionAdoptionRequest request) {
        if ("DIRECT_MATCH".equals(item.getAdoptionStatus())) {
            if ("USER_ROLE".equals(item.getSource())) {
                return request.getAdoptUserRolePermissions() == null || Boolean.TRUE.equals(request.getAdoptUserRolePermissions());
            }
            return request.getAdoptDirectUserPermissions() == null || Boolean.TRUE.equals(request.getAdoptDirectUserPermissions());
        }
        if ("GROUP_INHERITED_MATCH".equals(item.getAdoptionStatus())) {
            return Boolean.TRUE.equals(request.getAdoptGroupInheritedPermissions())
                    && Boolean.TRUE.equals(request.getAcknowledgeGroupInherited());
        }
        return false;
    }

    private String blockedMessage(HistoricalPermissionAdoptionPreview.Item item) {
        if ("GROUP_INHERITED_MATCH".equals(item.getAdoptionStatus())) {
            return "LDAP 组继承权限未确认，已阻止接管";
        }
        if ("ROLE_MISSING_LIVE".equals(item.getAdoptionStatus())) {
            return "角色权限在 live 中不存在，接管不会自动补发";
        }
        if ("LIVE_EXTRA_DIRECT".equals(item.getAdoptionStatus())) {
            return "角色外历史权限不会接管到当前角色";
        }
        return "该权限项不能接管";
    }

    private void addResultItem(HistoricalPermissionAdoptionResult result, String key, String action, String message,
                               Long recordId, String source, String sourceRole, String sourceGroup) {
        HistoricalPermissionAdoptionResult.Item item = new HistoricalPermissionAdoptionResult.Item();
        item.setKey(key);
        item.setAction(action);
        item.setMessage(message);
        item.setRecordId(recordId);
        item.setSource(source);
        item.setSourceRole(sourceRole);
        item.setSourceGroup(sourceGroup);
        result.getItems().add(item);
        if ("ADOPTED".equals(action)) {
            result.setAdoptedCount(result.getAdoptedCount() + 1);
        } else if ("SKIPPED_EXISTING".equals(action)) {
            result.setSkippedExistingCount(result.getSkippedExistingCount() + 1);
        } else if ("BLOCKED".equals(action)) {
            result.setBlockedCount(result.getBlockedCount() + 1);
        }
    }

    private void addWarnings(HistoricalPermissionAdoptionPreview preview) {
        String status = preview.getReconciliationStatus();
        if ("EXACT_MATCH".equals(status)) {
            preview.getWarnings().add("历史用户权限与角色范围完全一致，接管只写入 DGA 记录，不修改 Hive/Sentry。");
        } else if ("SUBSET_OF_ROLE".equals(status)) {
            preview.getWarnings().add("历史用户权限少于角色范围，接管不会补发角色中缺失的后端权限。");
        } else if ("SUPERSET_OF_ROLE".equals(status)) {
            preview.getWarnings().add("历史用户权限超过角色范围，角色外权限将继续保持 live-only/unmanaged。");
        } else if ("PARTIAL_OVERLAP".equals(status)) {
            preview.getWarnings().add("历史权限与角色范围部分重叠，系统只接管重叠部分。");
        } else if ("GROUP_INHERITED_ONLY".equals(status)) {
            preview.getWarnings().add("可匹配权限均来自 LDAP 组继承，默认不作为用户私有权限接管。");
        } else if ("NO_OVERLAP".equals(status)) {
            preview.getWarnings().add("当前角色与历史用户权限没有可接管交集，请选择更匹配的角色或先调整角色范围。");
        } else if ("ALREADY_ADOPTED".equals(status)) {
            preview.getWarnings().add("当前角色范围已存在 DGA 记录，重复接管会跳过。");
        }
    }

    private String snapshotHash(HistoricalPermissionAdoptionPreview preview) {
        List<String> parts = new ArrayList<>();
        parts.add(preview.getRoleCode());
        parts.add(preview.getUsername());
        parts.add(preview.getCluster());
        parts.add(preview.getAuthBackend());
        for (HistoricalPermissionAdoptionPreview.Item item : preview.getItems()) {
            parts.add(item.getKey() + ":" + item.getVerificationStatus() + ":" + item.getAdoptionStatus() + ":"
                    + firstNonBlank(item.getSource(), "") + ":" + firstNonBlank(item.getSourceRole(), "") + ":"
                    + firstNonBlank(item.getSourceGroup(), ""));
        }
        Collections.sort(parts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf(parts.hashCode());
        }
    }

    private String key(PermissionRecord record) {
        if (record == null || record.databaseName == null || record.permission == null) {
            return null;
        }
        return firstNonBlank(record.resourceType, record.tableName == null ? "DATABASE" : "TABLE").toUpperCase(Locale.ROOT)
                + "|" + record.databaseName.toLowerCase(Locale.ROOT)
                + "|" + firstNonBlank(record.tableName, "*").toLowerCase(Locale.ROOT)
                + "|" + record.permission.toUpperCase(Locale.ROOT)
                + "|" + firstNonBlank(record.authBackend, "").toUpperCase(Locale.ROOT);
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private boolean isGroupInherited(HistoricalPermissionAdoptionPreview.Item item) {
        return "GROUP_ROLE".equalsIgnoreCase(item.getSource());
    }

    private PermissionRecord firstRecord(PermissionRecord... records) {
        for (PermissionRecord record : records) {
            if (record != null) {
                return record;
            }
        }
        return null;
    }

    private Map<String, Object> lowerCaseKeys(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        if (row == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        return result;
    }

    private String normalizedResourceType(String resourceType, String tableName) {
        String normalized = trimToNull(resourceType);
        if (normalized == null) {
            return normalizeTable(tableName) == null ? "DATABASE" : "TABLE";
        }
        return normalized.toUpperCase(Locale.ROOT);
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

    private String normalizeTable(String table) {
        String normalized = trimToNull(table);
        if (normalized == null || "*".equals(normalized) || "ALL TABLES".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeAuthBackend(String authBackend) {
        String normalized = trimToNull(authBackend);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeSource(String source) {
        String normalized = trimToNull(source);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String riskLevelFor(String permission) {
        String normalized = normalizePermission(permission);
        if ("ALL".equals(normalized) || "DROP".equals(normalized) || "ALTER".equals(normalized)) {
            return "HIGH";
        }
        if ("INSERT".equals(normalized) || "UPDATE".equals(normalized) || "DELETE".equals(normalized)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String requireText(String value, String message) {
        String text = trimToNull(value);
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String text = trimToNull(value);
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
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stringValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private static class PreviewContext {
        private AuthRole role;
        private String cluster;
        private String resolvedCluster;
        private String authBackend;
        private String username;
        private String subjectType;
        private String subjectName;
        private boolean includeGroupInherited;
        private List<AuthRolePermission> rolePermissions = new ArrayList<>();
        private List<Map<String, Object>> livePermissions = new ArrayList<>();
        private List<UserResourceAccess> recordedAccess = new ArrayList<>();
        private Map<String, PermissionRecord> recordedByKey = new LinkedHashMap<>();
    }

    private static class PermissionRecord {
        private String key;
        private String resourceType;
        private String databaseName;
        private String tableName;
        private String permission;
        private String authBackend;
        private String source;
        private String sourceRole;
        private String sourceGroup;
        private String grantText;
    }
}
