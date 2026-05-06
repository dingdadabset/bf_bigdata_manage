package com.dga.access.service;

import com.dga.access.dto.AccessOwnerRequest;
import com.dga.access.entity.AccessActivityEvidence;
import com.dga.access.entity.AccessGovernanceIssue;
import com.dga.access.entity.AccessOwner;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.User;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.AccessActivityEvidenceRepository;
import com.dga.access.repository.AccessGovernanceIssueRepository;
import com.dga.access.repository.AccessOwnerRepository;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.repository.UserRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import com.dga.cluster.service.HiveServer2ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.criteria.Predicate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AccessGovernanceService {

    public static final List<String> SOURCE_SYSTEMS = Collections.unmodifiableList(Arrays.asList(
            "HIVE_SERVER2", "RANGER", "HDFS", "YARN", "STARROCKS", "DORIS"
    ));

    private static final Set<String> HIGH_PRIVILEGE_PERMISSIONS = new HashSet<>(Arrays.asList(
            "ALL", "ADMIN", "CREATE", "ALTER", "DROP", "GRANT", "OWNERSHIP"
    ));
    private static final List<String> RANGER_AUDIT_TABLE_CANDIDATES = Collections.unmodifiableList(Arrays.asList(
            "x_access_audit", "xa_access_audit"
    ));
    private static final int AUDIT_QUERY_BATCH_SIZE = 200;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SQL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LDAP_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .appendLiteral('Z')
            .toFormatter()
            .withZone(ZoneOffset.UTC);

    @Autowired
    private DgaUserRepository dgaUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private AccessGovernanceIssueRepository issueRepository;

    @Autowired
    private AccessActivityEvidenceRepository evidenceRepository;

    @Autowired
    private AccessOwnerRepository ownerRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private LdapService ldapService;

    @Autowired
    private RangerService rangerService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private HiveServer2ConnectionService hiveServer2ConnectionService;

    @Transactional
    public Map<String, Object> scan(String cluster, int inactiveDays, int reviewDays, String sourceText) {
        String clusterName = resolveClusterName(cluster);
        String clusterFilter = clusterName != null ? clusterName : clean(cluster);
        Set<String> selectedSources = normalizeSources(sourceText);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inactiveCutoff = now.minusDays(Math.max(inactiveDays, 1));
        LocalDateTime reviewCutoff = now.minusDays(Math.max(reviewDays, 1));
        List<DgaUser> users = dgaUserRepository.findActiveUsersByCluster(clusterFilter);
        Map<String, Object> collection = collectActivityEvidence(cluster, users, selectedSources, inactiveDays);
        int created = 0;
        int refreshed = 0;

        List<UserResourceAccess> activeAccesses = userResourceAccessRepository.findActiveByCluster(clean(cluster));
        closeUserLifecycleIssues(clean(cluster), "governance-scan");
        PermissionUsageResult usageResult = collectDatabasePermissionUsage(cluster, activeAccesses, selectedSources, inactiveDays);
        for (UserResourceAccess access : activeAccesses) {
            closeOpenIssue(issueKey("HIGH_PRIVILEGE_REVIEW", String.valueOf(access.getId())), "governance-scan");
            closeOpenIssue(issueKey("UNOWNED_PERMISSION", String.valueOf(access.getId())), "governance-scan");
        }
        closeLegacyPermissionScopedIssues(clean(cluster), "governance-scan");
        for (List<UserResourceAccess> accountAccesses : accessByAccount(activeAccesses).values()) {
            closeOpenIssue(accountIssueKey("UNUSED_DATABASE_PERMISSION", accountAccesses), "governance-scan");
            Boolean unusedPermissionIssue = scanUnusedDatabasePermissions(accountAccesses, usageResult, inactiveDays);
            if (Boolean.TRUE.equals(unusedPermissionIssue)) {
                created++;
            } else if (Boolean.FALSE.equals(unusedPermissionIssue)) {
                refreshed++;
            }
            Boolean highPrivilegeIssue = scanHighPrivilegeAccount(accountAccesses, reviewCutoff, reviewDays);
            if (Boolean.TRUE.equals(highPrivilegeIssue)) {
                created++;
            } else if (Boolean.FALSE.equals(highPrivilegeIssue)) {
                refreshed++;
            }
            Boolean unownedIssue = scanUnownedAccount(accountAccesses);
            if (Boolean.TRUE.equals(unownedIssue)) {
                created++;
            } else if (Boolean.FALSE.equals(unownedIssue)) {
                refreshed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("refreshed", refreshed);
        result.put("inactiveDays", inactiveDays);
        result.put("reviewDays", reviewDays);
        result.put("cluster", clusterName != null ? clusterName : clean(cluster));
        result.put("sourceSystems", selectedSources);
        result.put("sourceStatuses", collection.get("sourceStatuses"));
        result.put("evidenceCount", collection.get("evidenceCount"));
        result.put("scannedAt", now);
        return result;
    }

    public Page<AccessGovernanceIssue> listIssues(String cluster,
                                                  String issueTypes,
                                                  String status,
                                                  String username,
                                                  Pageable pageable) {
        Specification<AccessGovernanceIssue> spec = issueSpec(cluster, issueTypes, status, username);
        return issueRepository.findAll(spec, pageable);
    }

    public Map<String, Object> summary(String cluster) {
        List<AccessGovernanceIssue> issues = issueRepository.findUnresolvedByCluster(clean(cluster));
        Map<String, Integer> byType = new TreeMap<>();
        Map<String, Integer> bySeverity = new TreeMap<>();
        Map<String, Integer> byStatus = new TreeMap<>();
        Set<String> sources = new TreeSet<>();
        for (AccessGovernanceIssue issue : issues) {
            increment(byType, issue.getIssueType());
            increment(bySeverity, issue.getSeverity());
            increment(byStatus, issue.getStatus());
            if (issue.getSourceSystems() != null) {
                for (String source : issue.getSourceSystems().split(",")) {
                    if (!source.trim().isEmpty()) {
                        sources.add(source.trim());
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", issues.size());
        result.put("byType", byType);
        result.put("bySeverity", bySeverity);
        result.put("byStatus", byStatus);
        result.put("sourceSystems", SOURCE_SYSTEMS);
        result.put("sourceStatuses", sourceStatuses(cluster, new LinkedHashSet<>(SOURCE_SYSTEMS)));
        result.put("activeSources", sources);
        return result;
    }

    @Transactional
    public Map<String, Object> clearResults(String cluster, boolean includeEvidence, String operator) {
        String clusterFilter = clean(cluster);
        long issueCount = issueRepository.countByCluster(clusterFilter);
        long evidenceCount = includeEvidence ? evidenceRepository.countByCluster(clusterFilter) : 0;
        int deletedIssues = issueRepository.deleteByCluster(clusterFilter);
        int deletedEvidence = includeEvidence ? evidenceRepository.deleteByCluster(clusterFilter) : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("cluster", clusterFilter == null ? "ALL" : clusterFilter);
        result.put("deletedIssues", deletedIssues);
        result.put("deletedEvidence", deletedEvidence);
        result.put("issueCountBeforeDelete", issueCount);
        result.put("evidenceCountBeforeDelete", evidenceCount);
        result.put("operator", operator);
        result.put("clearedAt", LocalDateTime.now());
        return result;
    }

    public List<Map<String, Object>> sourceStatuses(String cluster, Set<String> selectedSources) {
        Cluster resolvedCluster = resolveClusterObject(cluster);
        boolean aggregateScope = resolvedCluster == null && isAllClusterScope(cluster);
        List<Map<String, Object>> statuses = new ArrayList<>();
        for (String source : selectedSources) {
            Map<String, Object> item = new HashMap<>();
            item.put("sourceSystem", source);
            item.put("label", sourceLabel(source));
            item.put("configured", aggregateScope
                    ? isSourceConfiguredForAnyCluster(source)
                    : isSourceConfigured(resolvedCluster, source));
            item.put("enabled", true);
            item.put("message", aggregateScope
                    ? sourceStatusMessageForAll(source)
                    : sourceStatusMessage(resolvedCluster, source));
            statuses.add(item);
        }
        return statuses;
    }

    public Set<String> normalizeSourcesForApi(String sourceText) {
        return normalizeSources(sourceText);
    }

    public List<Map<String, Object>> searchOwners(String query, String cluster) {
        String q = clean(query);
        String search = q == null ? "" : q;
        LinkedHashMap<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (AccessOwner owner : ownerRepository.searchOwners(search)) {
            result.put(owner.getOwnerCode().toLowerCase(Locale.ROOT), ownerMap(owner));
            if (result.size() >= 30) {
                return new ArrayList<>(result.values());
            }
        }
        for (User user : platformOwnerCandidates(search)) {
            String code = user.getUsername();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            result.put(code.toLowerCase(Locale.ROOT), ownerMap(code, firstNonBlank(user.getNickname(), code),
                    user.getEmail(), "PLATFORM_USER"));
            if (result.size() >= 30) {
                return new ArrayList<>(result.values());
            }
        }
        if (search.length() >= 2 && clean(cluster) != null) {
            try {
                for (Map<String, Object> ldapUser : ldapService.listUsers(cluster)) {
                    String uid = stringValue(ldapUser.get("uid"));
                    if (uid == null || !uid.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    result.put(uid.toLowerCase(Locale.ROOT), ownerMap(uid,
                            firstNonBlank(stringValue(ldapUser.get("cn")), uid),
                            stringValue(ldapUser.get("mail")), "LDAP"));
                    if (result.size() >= 30) {
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Owner search should stay usable even when LDAP is temporarily unavailable.
            }
        }
        return new ArrayList<>(result.values());
    }

    @Transactional
    public AccessOwner createOwner(AccessOwnerRequest request, String operator) {
        return ensureOwner(request == null ? null : request.getOwnerCode(),
                request == null ? null : request.getDisplayName(),
                request == null ? null : request.getEmail(),
                request == null ? "MANUAL" : firstNonBlank(request.getSource(), "MANUAL"),
                operator);
    }

    @Transactional
    public AccessGovernanceIssue claimIssue(Long issueId, String owner, List<String> collaboratorOwners,
                                            String assignee, String operator) {
        AccessGovernanceIssue issue = requireIssue(issueId);
        String resolvedOwner = firstNonBlank(owner, assignee, operator);
        issue.setOwner(resolvedOwner);
        issue.setCollaboratorOwners(joinOwners(collaboratorOwners));
        issue.setAssignee(firstNonBlank(assignee, resolvedOwner));
        issue.setStatus("CLAIMED");
        if (resolvedOwner != null) {
            ensureOwner(resolvedOwner, resolvedOwner, null, "MANUAL", operator);
        }
        ensureOwners(collaboratorOwners, operator);
        if ("UNOWNED_PERMISSION".equals(issue.getIssueType()) && resolvedOwner != null) {
            applyOwnerToAccountPermissions(issue, resolvedOwner, collaboratorOwners);
            issue.setStatus("RESOLVED");
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolvedBy(operator);
        }
        return issueRepository.save(issue);
    }

    @Transactional
    public AccessGovernanceIssue resolveIssue(Long issueId, String operator) {
        AccessGovernanceIssue issue = requireIssue(issueId);
        issue.setStatus("RESOLVED");
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolvedBy(operator);
        return issueRepository.save(issue);
    }

    @Transactional
    public UserResourceAccess setPermissionOwner(Long accessId, String owner, List<String> collaboratorOwners,
                                                 String displayName, String email, String operator) {
        if (owner == null || owner.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入主负责人");
        }
        UserResourceAccess access = userResourceAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "权限记录不存在: " + accessId));
        AccessOwner primary = ensureOwner(owner, firstNonBlank(displayName, owner), email, "MANUAL", operator);
        ensureOwners(collaboratorOwners, operator);
        access.setOwner(primary.getOwnerCode());
        access.setCollaboratorOwners(joinOwners(collaboratorOwners));
        UserResourceAccess saved = userResourceAccessRepository.save(access);
        closeOpenIssue(issueKey("UNOWNED_PERMISSION", String.valueOf(accessId)), operator);
        return saved;
    }

    @Transactional
    public UserResourceAccess markPermissionReviewed(Long accessId, int reviewDays, String operator) {
        UserResourceAccess access = userResourceAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "权限记录不存在: " + accessId));
        LocalDateTime now = LocalDateTime.now();
        access.setReviewedBy(operator);
        access.setLastReviewedAt(now);
        access.setReviewDueAt(now.plusDays(Math.max(reviewDays, 1)));
        UserResourceAccess saved = userResourceAccessRepository.save(access);
        closeOpenIssue(issueKey("HIGH_PRIVILEGE_REVIEW", String.valueOf(accessId)), operator);
        return saved;
    }

    @Transactional
    public AccessGovernanceIssue markAccountReviewed(Long issueId, int reviewDays, String operator) {
        AccessGovernanceIssue issue = requireIssue(issueId);
        if (!"HIGH_PRIVILEGE_REVIEW".equals(issue.getIssueType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅高权限复核风险支持该操作");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (UserResourceAccess access : activeAccountAccesses(issue)) {
            if (!isHighPrivilege(access.getPermission())) {
                continue;
            }
            access.setReviewedBy(operator);
            access.setLastReviewedAt(now);
            access.setReviewDueAt(now.plusDays(Math.max(reviewDays, 1)));
            userResourceAccessRepository.save(access);
            updated++;
        }
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该账号需要复核的高权限记录");
        }
        issue.setStatus("RESOLVED");
        issue.setResolvedAt(now);
        issue.setResolvedBy(operator);
        return issueRepository.save(issue);
    }

    private Map<String, Object> collectActivityEvidence(String cluster, List<DgaUser> users, Set<String> sources,
                                                        int inactiveDays) {
        List<Map<String, Object>> statuses = sourceStatuses(cluster, sources);
        int evidenceCount = 0;
        for (Map<String, Object> status : statuses) {
            String source = stringValue(status.get("sourceSystem"));
            if (!Boolean.TRUE.equals(status.get("configured"))) {
                continue;
            }
            try {
                String configuredMessage = stringValue(status.get("message"));
                int collected = collectSource(cluster, users, source, inactiveDays);
                evidenceCount += collected;
                status.put("message", configuredMessage == null
                        ? "采集完成，证据 " + collected + " 条"
                        : "采集完成，证据 " + collected + " 条：" + configuredMessage);
            } catch (Exception e) {
                status.put("configured", true);
                status.put("message", "采集失败: " + readableMessage(e));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sourceStatuses", statuses);
        result.put("evidenceCount", evidenceCount);
        return result;
    }

    private int collectSource(String cluster, List<DgaUser> users, String source, int inactiveDays) {
        if ("DGA".equals(source)) {
            return collectDgaEvidence(users);
        }
        if ("LDAP".equals(source)) {
            return collectLdapEvidence(cluster);
        }
        if ("RANGER".equals(source)) {
            return collectRangerEvidence(cluster);
        }
        if ("HIVE_SERVER2".equals(source)) {
            return collectHiveServer2Evidence(cluster, users);
        }
        if ("STARROCKS".equals(source)) {
            return collectSqlEngineEvidence(cluster, users, "STARROCKS", inactiveDays);
        }
        if ("DORIS".equals(source)) {
            return collectSqlEngineEvidence(cluster, users, "DORIS", inactiveDays);
        }
        if ("YARN".equals(source)) {
            return collectYarnEvidence(cluster, users);
        }
        if ("HDFS".equals(source)) {
            return collectHdfsEvidence(cluster, users);
        }
        if ("HUE".equals(source)) {
            return collectHueEvidence(cluster, users);
        }
        return 0;
    }

    private int collectDgaEvidence(List<DgaUser> users) {
        int count = 0;
        for (DgaUser user : users) {
            User platformUser = userRepository.findByUsername(user.getUsername());
            LocalDateTime platformLogin = platformUser == null ? null : platformUser.getLastLoginTime();
            LocalDateTime lastActive = firstNonNull(platformLogin, user.getLastActiveAt(), user.getUpdateTime(), user.getCreateTime());
            String confidence = platformLogin == null ? "LOW" : "HIGH";
            String evidence = platformLogin == null
                    ? "DGA 未找到平台登录时间，使用账号更新时间/创建时间兜底"
                    : "DGA 平台最近登录时间 " + formatTime(platformLogin);
            saveEvidence(user.getUsername(), user.getClusterName(), clusterCode(user.getClusterName()), "DGA",
                    lastActive, evidence, confidence, "OBSERVED", null);
            count++;
        }
        return count;
    }

    private int collectLdapEvidence(String cluster) {
        int count = 0;
        for (Map<String, Object> row : ldapService.listUsers(cluster)) {
            String username = stringValue(row.get("uid"));
            if (username == null) {
                continue;
            }
            String clusterName = resolveClusterName(cluster);
            LocalDateTime active = latest(parseTime(row.get("krbLastSuccessfulAuth")),
                    parseTime(row.get("authTimestamp")),
                    parseTime(row.get("lastLoginTime")),
                    parseTime(row.get("lastLogin")),
                    parseWindowsFileTime(row.get("lastLogonTimestamp")),
                    parseWindowsFileTime(row.get("lastLogon")));
            String metadataSummary = "modify=" + formatOptionalTime(parseTime(row.get("modifyTimestamp")))
                    + ", pwdChanged=" + formatOptionalTime(parseTime(row.get("pwdChangedTime")))
                    + ", create=" + formatOptionalTime(parseTime(row.get("createTimestamp")));
            String evidence = active == null
                    ? "LDAP entry exists, but no login/auth timestamp was available; metadata " + metadataSummary
                    : "LDAP login/auth timestamp " + formatTime(active);
            saveEvidence(username, clusterName, clusterCode(clusterName), "LDAP", active, evidence,
                    active == null ? "LOW" : "MEDIUM", active == null ? "PRESENT" : "OBSERVED", null);
            count++;
        }
        return count;
    }

    private int collectRangerEvidence(String cluster) {
        ClusterEndpoint auditEndpoint = rangerAuditEndpoint(cluster);
        if (auditEndpoint != null) {
            return collectRangerAuditEvidence(cluster, auditEndpoint);
        }
        ClusterEndpoint endpoint = endpoint(cluster, ClusterEndpoint.TYPE_RANGER);
        int count = 0;
        for (String username : rangerService.listPolicyUsers(endpoint)) {
            String clusterName = resolveClusterName(cluster);
            saveEvidence(username, clusterName, clusterCode(clusterName), "RANGER", null,
                    "Ranger policy contains this user; Ranger does not expose user activity time here",
                    "LOW", "PRESENT", null);
            count++;
        }
        return count;
    }

    private int collectRangerAuditEvidence(String cluster, ClusterEndpoint endpoint) {
        List<String> users = dgaUserRepository.findActiveUsersByCluster(resolveClusterName(cluster)).stream()
                .map(DgaUser::getUsername)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (users.isEmpty()) {
            return 0;
        }
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
        String auditTable = rangerAuditTable(endpoint);
        String serviceName = rangerServiceName(cluster, endpoint);
        String sql = "SELECT `request_user` AS username, MAX(`event_time`) AS last_active_at, "
                + "COUNT(*) AS hit_count, MAX(`resource_path`) AS sample"
                + " FROM " + quotedTableName(auditTable)
                + " WHERE `request_user` IN (" + quotedSqlValues(users) + ")"
                + (serviceName == null ? "" : " AND `repo_name` = " + quotedSqlValues(Collections.singletonList(serviceName)))
                + " GROUP BY `request_user`";
        int count = 0;
        String clusterName = resolveClusterName(cluster);
        String clusterCode = clusterCode(clusterName);
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
            String username = clean(stringValue(row.get("username")));
            if (username == null) {
                continue;
            }
            LocalDateTime lastActiveAt = parseTime(row.get("last_active_at"));
            String evidence = "Ranger Audit MySQL " + auditTable + " 最近访问 " + formatOptionalTime(lastActiveAt)
                    + "，命中 " + longValue(row.get("hit_count")) + " 条"
                    + (clean(stringValue(row.get("sample"))) == null
                    ? "" : "，样例资源 " + trimTo(stringValue(row.get("sample")), 120));
            saveEvidence(username, clusterName, clusterCode, "RANGER", lastActiveAt,
                    evidence, lastActiveAt == null ? "LOW" : "HIGH", "OBSERVED", null);
            count++;
        }
        return count;
    }

    private int collectHiveServer2Evidence(String cluster, List<DgaUser> users) {
        int count = 0;
        for (DgaUser user : users) {
            try {
                List<Map<String, Object>> permissions = authorizationService.getUserPermissions(user.getUsername(), cluster);
                if (permissions != null && !permissions.isEmpty()) {
                    String clusterName = resolveClusterName(cluster);
                    saveEvidence(user.getUsername(), clusterName, clusterCode(clusterName), "HIVE_SERVER2", null,
                            "HiveServer2/Sentry grants visible: " + permissions.size(),
                            "LOW", "PRESENT", null);
                    count++;
                }
            } catch (Exception ignored) {
                // Per-user grant lookup failures should not stop source collection.
            }
        }
        return count;
    }

    private int collectSqlEngineEvidence(String cluster, List<DgaUser> users, String source, int inactiveDays) {
        SqlAuditCollection collection = collectSqlAuditEvidence(cluster, users, source, inactiveDays);
        String clusterName = resolveClusterName(cluster);
        String clusterCode = clusterCode(clusterName);
        LocalDateTime inactiveCutoff = LocalDateTime.now().minusDays(Math.max(inactiveDays, 1));
        int count = 0;
        for (DgaUser user : users) {
            String username = clean(user.getUsername());
            if (username == null || !collection.candidateUsers.contains(username.toLowerCase(Locale.ROOT))) {
                continue;
            }
            AuditHit hit = collection.hitsByUser.get(username.toLowerCase(Locale.ROOT));
            if (hit != null && hit.lastActiveAt != null) {
                String sample = clean(hit.sample) == null ? "" : "，示例 " + hit.sample;
                saveEvidence(username, clusterName, clusterCode, source, hit.lastActiveAt,
                        source + " 审计表 " + collection.tableName + " 最近命中 "
                                + formatTime(hit.lastActiveAt) + "，命中 " + hit.hitCount + " 条" + sample,
                        "HIGH", "OBSERVED", null);
            } else {
                saveEvidence(username, clusterName, clusterCode, source, inactiveCutoff.minusSeconds(1),
                        source + " 审计表 " + collection.tableName + " 在最近 "
                                + Math.max(inactiveDays, 1) + " 天没有查询记录",
                        "MEDIUM", "ABSENT", null);
            }
            count++;
        }
        return count;
    }

    private SqlAuditCollection collectSqlAuditEvidence(String cluster, List<DgaUser> users, String source,
                                                       int inactiveDays) {
        ClusterEndpoint endpoint = endpoint(cluster, sqlEndpointType(source));
        Set<String> candidates = sqlAuditCandidateUsers(cluster, users);
        if (candidates.isEmpty()) {
            return new SqlAuditCollection(new LinkedHashSet<>(), new HashMap<>(), auditTableName(endpoint, source));
        }

        RuntimeException lastFailure = null;
        for (AuditTableSpec spec : auditTableSpecs(endpoint, source)) {
            try {
                Map<String, AuditHit> hits = queryAuditTable(endpoint, spec, candidates, inactiveDays);
                return new SqlAuditCollection(candidates, hits, spec.tableName);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        String configured = firstNonBlank(endpoint.getServiceName(), auditTableName(endpoint, source));
        throw new IllegalStateException(source + " 审计表查询失败，请确认 JDBC 账号可读审计表，"
                + "或在端点服务名配置审计表名，例如 " + configured
                + "；原因: " + (lastFailure == null ? "未知错误" : readableMessage(lastFailure)));
    }

    private Set<String> sqlAuditCandidateUsers(String cluster, List<DgaUser> users) {
        LinkedHashSet<String> userSet = new LinkedHashSet<>();
        Set<String> activeUsers = new HashSet<>();
        for (DgaUser user : users) {
            String username = clean(user.getUsername());
            if (username != null) {
                activeUsers.add(username.toLowerCase(Locale.ROOT));
            }
        }
        try {
            for (String principal : authorizationService.listPrincipals(cluster)) {
                String username = clean(principal);
                if (username != null && activeUsers.contains(username.toLowerCase(Locale.ROOT))) {
                    userSet.add(username.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ignored) {
            // Principal listing may require extra privileges; fall back to active DGA users.
        }
        if (!userSet.isEmpty()) {
            return userSet;
        }
        userSet.addAll(activeUsers);
        return userSet;
    }

    private Map<String, AuditHit> queryAuditTable(ClusterEndpoint endpoint, AuditTableSpec spec,
                                                  Set<String> candidateUsers, int inactiveDays) {
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint);
        Map<String, AuditHit> hits = new HashMap<>();
        List<String> users = new ArrayList<>(candidateUsers);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(inactiveDays, 1));
        for (int start = 0; start < users.size(); start += AUDIT_QUERY_BATCH_SIZE) {
            List<String> batch = users.subList(start, Math.min(start + AUDIT_QUERY_BATCH_SIZE, users.size()));
            String sql = buildAuditSql(spec, batch, cutoff);
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                String username = clean(stringValue(row.get("username")));
                LocalDateTime lastActiveAt = parseTime(row.get("last_active_at"));
                if (username == null || lastActiveAt == null) {
                    continue;
                }
                AuditHit hit = new AuditHit();
                hit.username = username;
                hit.lastActiveAt = lastActiveAt;
                hit.hitCount = longValue(row.get("hit_count"));
                hit.sample = trimTo(firstNonBlank(stringValue(row.get("sample_state")),
                        stringValue(row.get("sample_db")),
                        stringValue(row.get("sample_query_id")),
                        stringValue(row.get("sample_stmt"))), 160);
                hits.put(username.toLowerCase(Locale.ROOT), hit);
            }
        }
        return hits;
    }

    private String buildAuditSql(AuditTableSpec spec, List<String> users, LocalDateTime cutoff) {
        String userColumn = quotedIdentifier(spec.userColumn);
        String timeColumn = quotedIdentifier(spec.timeColumn);
        List<String> selectParts = new ArrayList<>();
        selectParts.add(userColumn + " AS username");
        selectParts.add("MAX(" + timeColumn + ") AS last_active_at");
        selectParts.add("COUNT(*) AS hit_count");
        return "SELECT " + String.join(", ", selectParts)
                + " FROM " + quotedTableName(spec.tableName)
                + " WHERE " + userColumn + " IN (" + quotedSqlValues(users) + ")"
                + " AND " + timeColumn + " >= '" + cutoff.format(SQL_TIME_FORMATTER) + "'"
                + " GROUP BY " + userColumn;
    }

    private String quotedSqlValues(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String value : values) {
            quoted.add("'" + value.replace("'", "''") + "'");
        }
        return String.join(",", quoted);
    }

    private List<AuditTableSpec> auditTableSpecs(ClusterEndpoint endpoint, String source) {
        List<AuditTableSpec> specs = new ArrayList<>();
        String configuredTable = clean(endpoint.getServiceName());
        if (configuredTable != null) {
            specs.add(new AuditTableSpec(configuredTable,
                    "DORIS".equals(source) ? "time" : "timestamp",
                    "user", "db", "state",
                    "DORIS".equals(source) ? "query_id" : "queryId",
                    "stmt"));
        }
        if ("DORIS".equals(source)) {
            specs.add(new AuditTableSpec("doris_audit_db__.doris_audit_tbl__", "time",
                    "user", "db", "state", "query_id", "stmt"));
            specs.add(new AuditTableSpec("doris_audit_db__.doris_audit_tbl", "time",
                    "user", "db", "state", "query_id", "stmt"));
        } else {
            specs.add(new AuditTableSpec("starrocks_audit_db__.starrocks_audit_tbl__", "timestamp",
                    "user", "db", "state", "queryId", "stmt"));
            specs.add(new AuditTableSpec("starrocks_audit_db__.starrocks_audit_tbl", "timestamp",
                    "user", "db", "state", "queryId", "stmt"));
        }
        return specs;
    }

    private String auditTableName(ClusterEndpoint endpoint, String source) {
        String configured = endpoint == null ? null : clean(endpoint.getServiceName());
        if (configured != null) {
            return configured;
        }
        return "DORIS".equals(source)
                ? "doris_audit_db__.doris_audit_tbl__"
                : "starrocks_audit_db__.starrocks_audit_tbl__";
    }

    private JdbcTemplate sqlJdbcTemplate(ClusterEndpoint endpoint) {
        return sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
    }

    private JdbcTemplate sqlJdbcTemplate(ClusterEndpoint endpoint, String driverClassName) {
        if (ClusterEndpoint.TYPE_HIVE_SERVER2.equalsIgnoreCase(endpoint.getEndpointType())
                || "org.apache.hive.jdbc.HiveDriver".equals(driverClassName)) {
            return hiveServer2ConnectionService.jdbcTemplate(endpoint);
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(endpoint.getUrl());
        dataSource.setUsername(endpoint.getUsername());
        dataSource.setPassword(endpoint.getPassword());
        return new JdbcTemplate(dataSource);
    }

    private String sqlEndpointType(String source) {
        return "DORIS".equals(source) ? ClusterEndpoint.TYPE_DORIS_JDBC : ClusterEndpoint.TYPE_STARROCKS_JDBC;
    }

    private PermissionUsageResult collectDatabasePermissionUsage(String cluster,
                                                                 List<UserResourceAccess> accesses,
                                                                 Set<String> selectedSources,
                                                                 int inactiveDays) {
        PermissionUsageResult result = new PermissionUsageResult();
        if (selectedSources.contains("DORIS")) {
            collectSqlPermissionUsage(cluster, accesses, "DORIS", inactiveDays, result);
        }
        if (selectedSources.contains("STARROCKS")) {
            collectSqlPermissionUsage(cluster, accesses, "STARROCKS", inactiveDays, result);
        }
        if (selectedSources.contains("RANGER")) {
            collectRangerPermissionUsage(cluster, accesses, inactiveDays, result);
        }
        if (selectedSources.contains("HIVE_SERVER2") || selectedSources.contains("RANGER")) {
            collectHivePermissionUsage(cluster, accesses, inactiveDays, result);
        }
        if (selectedSources.contains("HDFS") || selectedSources.contains("YARN")) {
            collectHiveAuxiliaryUsage(cluster, accesses, selectedSources, inactiveDays, result);
        }
        return result;
    }

    private void collectSqlPermissionUsage(String cluster, List<UserResourceAccess> accesses, String source,
                                           int inactiveDays, PermissionUsageResult result) {
        List<UserResourceAccess> relevant = relevantDatabaseAccesses(accesses, source);
        if (relevant.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<UserResourceAccess>> entry : accessesByCluster(relevant, cluster).entrySet()) {
            try {
                ClusterEndpoint endpoint = endpoint(entry.getKey(), sqlEndpointType(source));
                List<AuditTableSpec> specs = auditTableSpecs(endpoint, source);
                Map<String, AuditHit> hits = queryPermissionAudit(endpoint, specs,
                        entry.getValue(), inactiveDays, "com.mysql.cj.jdbc.Driver");
                Map<String, AuditHit> historyHits;
                try {
                    historyHits = queryPermissionAudit(endpoint, specs,
                            entry.getValue(), null, "com.mysql.cj.jdbc.Driver");
                } catch (Exception ignored) {
                    historyHits = Collections.emptyMap();
                }
                markCoveredAndUsed(entry.getValue(), hits, historyHits, result);
            } catch (Exception ignored) {
                // Source collection status already surfaces audit connectivity failures; avoid false unused findings.
            }
        }
    }

    private void collectRangerPermissionUsage(String cluster, List<UserResourceAccess> accesses, int inactiveDays,
                                              PermissionUsageResult result) {
        List<UserResourceAccess> relevant = relevantDatabaseAccesses(accesses, "HIVE_SERVER2");
        if (relevant.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<UserResourceAccess>> entry : accessesByCluster(relevant, cluster).entrySet()) {
            try {
                ClusterEndpoint endpoint = endpoint(entry.getKey(), ClusterEndpoint.TYPE_RANGER_DB);
                String auditTable = rangerAuditTable(endpoint);
                if (!rangerAuditHasRows(entry.getKey(), endpoint, auditTable)) {
                    continue;
                }
                Map<String, AuditHit> hits = queryRangerPermissionAudit(entry.getKey(), endpoint,
                        entry.getValue(), inactiveDays, auditTable);
                Map<String, AuditHit> historyHits;
                try {
                    historyHits = queryRangerPermissionAudit(entry.getKey(), endpoint, entry.getValue(), null, auditTable);
                } catch (Exception ignored) {
                    historyHits = Collections.emptyMap();
                }
                markCoveredAndUsed(entry.getValue(), hits, historyHits, result);
            } catch (Exception ignored) {
                // Ranger policy endpoints do not expose audit history; only RANGER_DB provides direct coverage.
            }
        }
    }

    private void collectHivePermissionUsage(String cluster, List<UserResourceAccess> accesses, int inactiveDays,
                                            PermissionUsageResult result) {
        List<UserResourceAccess> relevant = relevantDatabaseAccesses(accesses, "HIVE_SERVER2");
        if (relevant.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<UserResourceAccess>> entry : accessesByCluster(relevant, cluster).entrySet()) {
            try {
                ClusterEndpoint endpoint = endpoint(entry.getKey(), ClusterEndpoint.TYPE_HIVE_SERVER2);
                String auditTable = clean(endpoint.getServiceName());
                if (auditTable == null) {
                    continue;
                }
                List<AuditTableSpec> specs = Collections.singletonList(
                        new AuditTableSpec(auditTable, "event_time", "user", "database", "table", null, "operation")
                );
                Map<String, AuditHit> hits = queryPermissionAudit(endpoint, specs, entry.getValue(), inactiveDays,
                        "org.apache.hive.jdbc.HiveDriver");
                Map<String, AuditHit> historyHits;
                try {
                    historyHits = queryPermissionAudit(endpoint, specs, entry.getValue(), null,
                            "org.apache.hive.jdbc.HiveDriver");
                } catch (Exception ignored) {
                    historyHits = Collections.emptyMap();
                }
                markCoveredAndUsed(entry.getValue(), hits, historyHits, result);
            } catch (Exception ignored) {
                // Hive audit schemas are deployment-specific; no audit coverage means no unused-permission finding.
            }
        }
    }

    private void collectHiveAuxiliaryUsage(String cluster,
                                           List<UserResourceAccess> accesses,
                                           Set<String> selectedSources,
                                           int inactiveDays,
                                           PermissionUsageResult result) {
        List<UserResourceAccess> relevant = relevantDatabaseAccesses(accesses, "HIVE_SERVER2");
        if (relevant.isEmpty()) {
            return;
        }
        if (selectedSources.contains("YARN")) {
            collectYarnAuxiliaryUsage(cluster, relevant, inactiveDays, result);
        }
        if (selectedSources.contains("HDFS")) {
            collectHdfsAuxiliaryUsage(cluster, relevant, inactiveDays, result);
        }
    }

    private void collectYarnAuxiliaryUsage(String cluster,
                                           List<UserResourceAccess> accesses,
                                           int inactiveDays,
                                           PermissionUsageResult result) {
        for (Map.Entry<String, List<UserResourceAccess>> entry : accessesByCluster(accesses, cluster).entrySet()) {
            try {
                ClusterEndpoint endpoint = endpoint(entry.getKey(), ClusterEndpoint.TYPE_YARN);
                Map<String, AuditHit> hits = isJdbcEndpoint(endpoint) && clean(endpoint.getServiceName()) != null
                        ? queryGenericUserActivity(endpoint, clean(endpoint.getServiceName()),
                        "user", "event_time", "application_id", "YARN", entry.getValue())
                        : queryYarnRestActivity(endpoint, entry.getValue());
                markAuxiliaryCovered(entry.getValue(), hits, inactiveDays, "YARN", result);
            } catch (Exception ignored) {
                // YARN is auxiliary evidence only; missing history must not fail the scan.
            }
        }
    }

    private void collectHdfsAuxiliaryUsage(String cluster,
                                           List<UserResourceAccess> accesses,
                                           int inactiveDays,
                                           PermissionUsageResult result) {
        for (Map.Entry<String, List<UserResourceAccess>> entry : accessesByCluster(accesses, cluster).entrySet()) {
            try {
                ClusterEndpoint endpoint = endpoint(entry.getKey(), ClusterEndpoint.TYPE_HDFS);
                Map<String, AuditHit> hits = isJdbcEndpoint(endpoint) && clean(endpoint.getServiceName()) != null
                        ? queryGenericUserActivity(endpoint, clean(endpoint.getServiceName()),
                        "user", "event_time", "path", "HDFS", entry.getValue())
                        : queryHdfsRestActivity(endpoint, entry.getValue());
                markAuxiliaryCovered(entry.getValue(), hits, inactiveDays, "HDFS", result);
            } catch (Exception ignored) {
                // HDFS is auxiliary evidence only; missing audit data must not fail the scan.
            }
        }
    }

    private Map<String, List<UserResourceAccess>> accessesByCluster(List<UserResourceAccess> accesses,
                                                                    String fallbackCluster) {
        Map<String, List<UserResourceAccess>> grouped = new LinkedHashMap<>();
        for (UserResourceAccess access : accesses) {
            String clusterKey = firstNonBlank(access.getClusterCode(), access.getClusterName(), fallbackCluster);
            clusterKey = clean(clusterKey);
            if (clusterKey == null) {
                continue;
            }
            grouped.computeIfAbsent(clusterKey, key -> new ArrayList<>()).add(access);
        }
        return grouped;
    }

    private Map<String, AuditHit> queryGenericUserActivity(ClusterEndpoint endpoint,
                                                           String tableName,
                                                           String userColumn,
                                                           String timeColumn,
                                                           String sampleColumn,
                                                           String sourceSystem,
                                                           List<UserResourceAccess> accesses) {
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
        Map<String, AuditHit> hits = new HashMap<>();
        List<String> users = new ArrayList<>(usernames(accesses));
        if (users.isEmpty()) {
            return hits;
        }
        String sql = "SELECT " + quotedIdentifier(userColumn) + " AS username, "
                + "MAX(" + quotedIdentifier(timeColumn) + ") AS last_active_at, "
                + "COUNT(*) AS hit_count"
                + (clean(sampleColumn) == null ? "" : ", MAX(" + quotedIdentifier(sampleColumn) + ") AS sample")
                + " FROM " + quotedTableName(tableName)
                + " WHERE " + quotedIdentifier(userColumn) + " IN (" + quotedSqlValues(users) + ")"
                + " GROUP BY " + quotedIdentifier(userColumn);
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
            AuditHit hit = new AuditHit();
            hit.username = clean(stringValue(row.get("username")));
            hit.databaseName = "*";
            hit.lastActiveAt = parseTime(row.get("last_active_at"));
            hit.hitCount = longValue(row.get("hit_count"));
            hit.sample = clean(stringValue(row.get("sample")));
            hit.sourceSystem = sourceSystem;
            hit.confidence = "LOW";
            if (hit.username != null) {
                hits.put(permissionAuditKey(hit.username, "*"), hit);
            }
        }
        return hits;
    }

    private Map<String, AuditHit> queryYarnRestActivity(ClusterEndpoint endpoint, List<UserResourceAccess> accesses) {
        Map<String, AuditHit> hits = new HashMap<>();
        for (String username : usernames(accesses)) {
            try {
                String url = trimTrailingSlash(endpoint.getUrl()) + "/ws/v1/cluster/apps?user="
                        + urlEncode(username) + "&states=RUNNING,FINISHED,KILLED,FAILED&limit=1";
                ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers(endpoint)), Map.class);
                List apps = nestedList(response.getBody(), "apps", "app");
                if (apps == null || apps.isEmpty()) {
                    continue;
                }
                Map app = (Map) apps.get(0);
                AuditHit hit = new AuditHit();
                hit.username = username;
                hit.databaseName = "*";
                hit.lastActiveAt = latest(parseTime(app.get("finishedTime")), parseTime(app.get("startedTime")));
                hit.hitCount = 1;
                hit.sample = "YARN app " + firstNonBlank(stringValue(app.get("id")), "-")
                        + " state=" + firstNonBlank(stringValue(app.get("state")), "-");
                hit.sourceSystem = "YARN";
                hit.confidence = "LOW";
                hits.put(permissionAuditKey(username, "*"), hit);
            } catch (Exception ignored) {
                // A single user's missing app history is expected in normal scans.
            }
        }
        return hits;
    }

    private Map<String, AuditHit> queryHdfsRestActivity(ClusterEndpoint endpoint, List<UserResourceAccess> accesses) {
        Map<String, AuditHit> hits = new HashMap<>();
        for (String username : usernames(accesses)) {
            try {
                String url = trimTrailingSlash(endpoint.getUrl()) + "/webhdfs/v1/user/"
                        + urlEncode(username) + "?op=GETFILESTATUS";
                ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers(endpoint)), Map.class);
                Map status = nestedMap(response.getBody(), "FileStatus");
                if (status == null) {
                    continue;
                }
                AuditHit hit = new AuditHit();
                hit.username = username;
                hit.databaseName = "*";
                hit.lastActiveAt = latest(parseTime(status.get("accessTime")), parseTime(status.get("modificationTime")));
                hit.hitCount = 1;
                hit.sample = "HDFS /user/" + username + " access/modification time";
                hit.sourceSystem = "HDFS";
                hit.confidence = "LOW";
                hits.put(permissionAuditKey(username, "*"), hit);
            } catch (Exception ignored) {
                // Missing home directories are normal and should not block governance scans.
            }
        }
        return hits;
    }

    private Set<String> usernames(List<UserResourceAccess> accesses) {
        Set<String> users = new LinkedHashSet<>();
        for (UserResourceAccess access : accesses) {
            String username = clean(access.getUsername());
            if (username != null) {
                users.add(username);
            }
        }
        return users;
    }

    private boolean isJdbcEndpoint(ClusterEndpoint endpoint) {
        return endpoint != null && clean(endpoint.getUrl()) != null
                && endpoint.getUrl().trim().toLowerCase(Locale.ROOT).startsWith("jdbc:");
    }

    private String rangerAuditTable(ClusterEndpoint endpoint) {
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
        for (String table : RANGER_AUDIT_TABLE_CANDIDATES) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '"
                            + table + "'",
                    Long.class);
            if (count != null && count > 0) {
                return table;
            }
        }
        throw new IllegalStateException("Ranger Audit 表不存在，已尝试 x_access_audit/xa_access_audit");
    }

    private boolean rangerAuditHasRows(String cluster, ClusterEndpoint endpoint, String auditTable) {
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
        String serviceName = rangerServiceName(cluster, endpoint);
        String sql = "SELECT COUNT(*) FROM " + quotedTableName(auditTable)
                + (serviceName == null ? "" : " WHERE `repo_name` = " + quotedSqlValues(Collections.singletonList(serviceName)));
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null && count > 0;
    }

    private Map<String, AuditHit> queryRangerPermissionAudit(String cluster,
                                                             ClusterEndpoint endpoint,
                                                             List<UserResourceAccess> accesses,
                                                             Integer inactiveDays,
                                                             String auditTable) {
        Set<String> users = usernames(accesses);
        if (users.isEmpty()) {
            return Collections.emptyMap();
        }
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, "com.mysql.cj.jdbc.Driver");
        Map<String, AuditHit> hits = new HashMap<>();
        List<String> userList = new ArrayList<>(users);
        LocalDateTime cutoff = inactiveDays == null ? null : LocalDateTime.now().minusDays(Math.max(inactiveDays, 1));
        for (int start = 0; start < userList.size(); start += AUDIT_QUERY_BATCH_SIZE) {
            List<String> batch = userList.subList(start, Math.min(start + AUDIT_QUERY_BATCH_SIZE, userList.size()));
            String sql = buildRangerAuditSql(cluster, endpoint, auditTable, batch, cutoff);
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                AuditHit hit = auditHit(row);
                hit.sourceSystem = "RANGER";
                hit.confidence = "HIGH";
                hit.sample = clean(stringValue(row.get("sample")));
                if (hit.username == null) {
                    continue;
                }
                hits.put(permissionAuditKey(hit.username, hit.databaseName), hit);
            }
        }
        return hits;
    }

    private String buildRangerAuditSql(String cluster, ClusterEndpoint endpoint, String auditTable, List<String> users,
                                       LocalDateTime cutoff) {
        String actionExpression = "LOWER(TRIM(COALESCE(`access_type`, `action`, `request_data`, '')))";
        String resourceExpression = "TRIM(BOTH '/' FROM `resource_path`)";
        String databaseExpression = "CASE "
                + "WHEN `resource_path` IS NULL OR TRIM(`resource_path`) = '' THEN '*' "
                + "WHEN LOCATE('/', " + resourceExpression + ") > 0 THEN SUBSTRING_INDEX("
                + resourceExpression + ", '/', 1) "
                + "WHEN LOCATE('.', " + resourceExpression + ") > 0 THEN SUBSTRING_INDEX("
                + resourceExpression + ", '.', 1) "
                + "ELSE " + resourceExpression + " END";
        String serviceName = rangerServiceName(cluster, endpoint);
        return "SELECT `request_user` AS username, "
                + databaseExpression + " AS database_name, "
                + "MAX(`event_time`) AS last_active_at, "
                + "COUNT(*) AS hit_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'select%' OR "
                + actionExpression + " LIKE 'read%' OR " + actionExpression + " LIKE 'show%' OR "
                + actionExpression + " LIKE 'query%' THEN 1 ELSE 0 END) AS select_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'insert%' OR "
                + actionExpression + " LIKE 'update%' OR " + actionExpression + " LIKE 'load%' OR "
                + actionExpression + " LIKE 'write%' THEN 1 ELSE 0 END) AS insert_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'create%' THEN 1 ELSE 0 END) AS create_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'alter%' THEN 1 ELSE 0 END) AS alter_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'drop%' THEN 1 ELSE 0 END) AS drop_count, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'select%' OR "
                + actionExpression + " LIKE 'read%' OR " + actionExpression + " LIKE 'show%' OR "
                + actionExpression + " LIKE 'query%' THEN `event_time` ELSE NULL END) AS select_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'insert%' OR "
                + actionExpression + " LIKE 'update%' OR " + actionExpression + " LIKE 'load%' OR "
                + actionExpression + " LIKE 'write%' THEN `event_time` ELSE NULL END) AS insert_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'create%' THEN `event_time` ELSE NULL END) AS create_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'alter%' THEN `event_time` ELSE NULL END) AS alter_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'drop%' THEN `event_time` ELSE NULL END) AS drop_last_at, "
                + "MAX(`resource_path`) AS sample"
                + " FROM " + quotedTableName(auditTable)
                + " WHERE `request_user` IN (" + quotedSqlValues(users) + ")"
                + (serviceName == null ? "" : " AND `repo_name` = " + quotedSqlValues(Collections.singletonList(serviceName)))
                + (cutoff == null ? "" : " AND `event_time` >= '" + cutoff.format(SQL_TIME_FORMATTER) + "'")
                + " GROUP BY `request_user`, " + databaseExpression;
    }

    private Map<String, AuditHit> queryPermissionAudit(ClusterEndpoint endpoint,
                                                       List<AuditTableSpec> specs,
                                                       List<UserResourceAccess> accesses,
                                                       Integer inactiveDays,
                                                       String driverClassName) {
        RuntimeException lastFailure = null;
        Set<String> users = new LinkedHashSet<>();
        for (UserResourceAccess access : accesses) {
            String username = clean(access.getUsername());
            if (username != null) {
                users.add(username.toLowerCase(Locale.ROOT));
            }
        }
        for (AuditTableSpec spec : specs) {
            try {
                return queryPermissionAuditTable(endpoint, spec, users, inactiveDays, driverClassName);
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        throw lastFailure == null ? new IllegalStateException("审计表查询失败") : lastFailure;
    }

    private Map<String, AuditHit> queryPermissionAuditTable(ClusterEndpoint endpoint,
                                                            AuditTableSpec spec,
                                                            Set<String> users,
                                                            Integer inactiveDays,
                                                            String driverClassName) {
        JdbcTemplate jdbcTemplate = sqlJdbcTemplate(endpoint, driverClassName);
        Map<String, AuditHit> hits = new HashMap<>();
        List<String> userList = new ArrayList<>(users);
        LocalDateTime cutoff = inactiveDays == null ? null : LocalDateTime.now().minusDays(Math.max(inactiveDays, 1));
        for (int start = 0; start < userList.size(); start += AUDIT_QUERY_BATCH_SIZE) {
            List<String> batch = userList.subList(start, Math.min(start + AUDIT_QUERY_BATCH_SIZE, userList.size()));
            String sql = buildPermissionAuditSql(spec, batch, cutoff);
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                AuditHit hit = auditHit(row);
                if (hit.username == null) {
                    continue;
                }
                hits.put(permissionAuditKey(hit.username, hit.databaseName), hit);
            }
        }
        return hits;
    }

    private String buildPermissionAuditSql(AuditTableSpec spec, List<String> users, LocalDateTime cutoff) {
        String userColumn = quotedIdentifier(spec.userColumn);
        String dbColumn = quotedIdentifier(spec.dbColumn);
        String timeColumn = quotedIdentifier(spec.timeColumn);
        String actionExpression = "LOWER(TRIM(" + quotedIdentifier(spec.stmtColumn) + "))";
        return "SELECT " + userColumn + " AS username, "
                + dbColumn + " AS database_name, "
                + "MAX(" + timeColumn + ") AS last_active_at, "
                + "COUNT(*) AS hit_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'select%' OR "
                + actionExpression + " LIKE 'with%' OR " + actionExpression + " LIKE 'show%' OR "
                + actionExpression + " LIKE 'query%' THEN 1 ELSE 0 END) AS select_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'insert%' OR "
                + actionExpression + " LIKE 'load%' OR " + actionExpression + " LIKE 'update%' THEN 1 ELSE 0 END) AS insert_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'create%' THEN 1 ELSE 0 END) AS create_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'alter%' THEN 1 ELSE 0 END) AS alter_count, "
                + "SUM(CASE WHEN " + actionExpression + " LIKE 'drop%' THEN 1 ELSE 0 END) AS drop_count, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'select%' OR "
                + actionExpression + " LIKE 'with%' OR " + actionExpression + " LIKE 'show%' OR "
                + actionExpression + " LIKE 'query%' THEN "
                + timeColumn + " ELSE NULL END) AS select_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'insert%' OR "
                + actionExpression + " LIKE 'load%' OR " + actionExpression + " LIKE 'update%' THEN "
                + timeColumn + " ELSE NULL END) AS insert_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'create%' THEN " + timeColumn
                + " ELSE NULL END) AS create_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'alter%' THEN " + timeColumn
                + " ELSE NULL END) AS alter_last_at, "
                + "MAX(CASE WHEN " + actionExpression + " LIKE 'drop%' THEN " + timeColumn
                + " ELSE NULL END) AS drop_last_at "
                + " FROM " + quotedTableName(spec.tableName)
                + " WHERE " + userColumn + " IN (" + quotedSqlValues(users) + ")"
                + (cutoff == null ? "" : " AND " + timeColumn + " >= '" + cutoff.format(SQL_TIME_FORMATTER) + "'")
                + " GROUP BY " + userColumn + ", " + dbColumn;
    }

    private AuditHit auditHit(Map<String, Object> row) {
        AuditHit hit = new AuditHit();
        hit.username = clean(stringValue(row.get("username")));
        hit.databaseName = clean(stringValue(row.get("database_name")));
        hit.lastActiveAt = parseTime(row.get("last_active_at"));
        hit.hitCount = longValue(row.get("hit_count"));
        hit.selectCount = longValue(row.get("select_count"));
        hit.insertCount = longValue(row.get("insert_count"));
        hit.createCount = longValue(row.get("create_count"));
        hit.alterCount = longValue(row.get("alter_count"));
        hit.dropCount = longValue(row.get("drop_count"));
        hit.selectLastAt = parseTime(row.get("select_last_at"));
        hit.insertLastAt = parseTime(row.get("insert_last_at"));
        hit.createLastAt = parseTime(row.get("create_last_at"));
        hit.alterLastAt = parseTime(row.get("alter_last_at"));
        hit.dropLastAt = parseTime(row.get("drop_last_at"));
        return hit;
    }

    private void markCoveredAndUsed(List<UserResourceAccess> accesses,
                                    Map<String, AuditHit> hits,
                                    Map<String, AuditHit> historyHits,
                                    PermissionUsageResult result) {
        for (UserResourceAccess access : accesses) {
            if (access.getId() == null) {
                continue;
            }
            result.coveredAccessIds.add(access.getId());
            result.directCoveredAccessIds.add(access.getId());
            AuditHit hit = findAuditHit(access, hits);
            if (hit != null && permissionUsed(access.getPermission(), hit)) {
                result.usedByAccessId.put(access.getId(), hit);
            }
            AuditHit historyHit = findAuditHit(access, historyHits);
            if (historyHit != null) {
                putLatestHistory(access.getId(), historyHit, result);
            }
        }
    }

    private void markAuxiliaryCovered(List<UserResourceAccess> accesses,
                                      Map<String, AuditHit> hits,
                                      int inactiveDays,
                                      String sourceSystem,
                                      PermissionUsageResult result) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(inactiveDays, 1));
        for (UserResourceAccess access : accesses) {
            if (access.getId() == null) {
                continue;
            }
            AuditHit hit = findAuditHit(access, hits);
            if (hit != null) {
                hit.sourceSystem = firstNonBlank(hit.sourceSystem, sourceSystem);
                hit.confidence = firstNonBlank(hit.confidence, "LOW");
                putLatestAuxiliary(access.getId(), hit, result);
                putLatestHistory(access.getId(), hit, result);
            }
            if (result.directCoveredAccessIds.contains(access.getId())) {
                continue;
            }
            result.coveredAccessIds.add(access.getId());
            if (hit != null && hit.lastActiveAt != null && !hit.lastActiveAt.isBefore(cutoff)) {
                result.usedByAccessId.put(access.getId(), hit);
            }
        }
    }

    private void putLatestHistory(Long accessId, AuditHit hit, PermissionUsageResult result) {
        AuditHit current = result.historyByAccessId.get(accessId);
        if (current == null || isAfter(hit.lastActiveAt, current.lastActiveAt)) {
            result.historyByAccessId.put(accessId, hit);
        }
    }

    private void putLatestAuxiliary(Long accessId, AuditHit hit, PermissionUsageResult result) {
        AuditHit current = result.auxiliaryByAccessId.get(accessId);
        if (current == null || isAfter(hit.lastActiveAt, current.lastActiveAt)) {
            result.auxiliaryByAccessId.put(accessId, hit);
        }
    }

    private boolean isAfter(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return false;
        }
        return right == null || left.isAfter(right);
    }

    private AuditHit findAuditHit(UserResourceAccess access, Map<String, AuditHit> hits) {
        String username = clean(access.getUsername());
        if (username == null) {
            return null;
        }
        String database = clean(access.getDatabaseName());
        AuditHit hit = database == null ? null : hits.get(permissionAuditKey(username, database));
        if (hit != null) {
            return hit;
        }
        return hits.get(permissionAuditKey(username, "*"));
    }

    private boolean permissionUsed(String permission, AuditHit hit) {
        String normalized = firstPermission(permission);
        if (normalized == null) {
            return hit.hitCount > 0;
        }
        if ("ALL".equals(normalized) || "ADMIN".equals(normalized) || "OWNERSHIP".equals(normalized)) {
            return hit.hitCount > 0;
        }
        if ("SELECT".equals(normalized)) {
            return hit.selectCount > 0;
        }
        if ("INSERT".equals(normalized) || "UPDATE".equals(normalized) || "LOAD".equals(normalized)) {
            return hit.insertCount > 0;
        }
        if ("CREATE".equals(normalized)) {
            return hit.createCount > 0;
        }
        if ("ALTER".equals(normalized)) {
            return hit.alterCount > 0;
        }
        if ("DROP".equals(normalized)) {
            return hit.dropCount > 0;
        }
        return hit.hitCount > 0;
    }

    private LocalDateTime lastPermissionUseAt(UserResourceAccess access, PermissionUsageResult usageResult) {
        if (access == null || access.getId() == null) {
            return null;
        }
        AuditHit hit = usageResult.historyByAccessId.get(access.getId());
        if (hit == null) {
            return null;
        }
        String normalized = firstPermission(access.getPermission());
        if (normalized == null || "ALL".equals(normalized) || "ADMIN".equals(normalized)
                || "OWNERSHIP".equals(normalized)) {
            return hit.lastActiveAt;
        }
        if ("SELECT".equals(normalized)) {
            return hit.selectLastAt;
        }
        if ("INSERT".equals(normalized) || "UPDATE".equals(normalized) || "LOAD".equals(normalized)) {
            return hit.insertLastAt;
        }
        if ("CREATE".equals(normalized)) {
            return hit.createLastAt;
        }
        if ("ALTER".equals(normalized)) {
            return hit.alterLastAt;
        }
        if ("DROP".equals(normalized)) {
            return hit.dropLastAt;
        }
        return hit.lastActiveAt;
    }

    private String firstPermission(String permission) {
        if (permission == null) {
            return null;
        }
        for (String part : permission.split(",")) {
            String normalized = clean(part);
            if (normalized != null) {
                return normalized.toUpperCase(Locale.ROOT).replace("_PRIV", "");
            }
        }
        return null;
    }

    private String permissionAuditKey(String username, String database) {
        return clusterValue(username).toLowerCase(Locale.ROOT) + "|"
                + clusterValue(firstNonBlank(database, "*")).toLowerCase(Locale.ROOT);
    }

    private List<UserResourceAccess> relevantDatabaseAccesses(List<UserResourceAccess> accesses, String source) {
        List<UserResourceAccess> relevant = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (access.getId() == null || clean(access.getUsername()) == null) {
                continue;
            }
            if (!isDatabaseAccess(access)) {
                continue;
            }
            if ("DORIS".equals(source) && "DORIS".equals(accessSource(access))) {
                relevant.add(access);
            } else if ("STARROCKS".equals(source) && "STARROCKS".equals(accessSource(access))) {
                relevant.add(access);
            } else if ("HIVE_SERVER2".equals(source) && isHiveAccess(access)) {
                relevant.add(access);
            }
        }
        return relevant;
    }

    private boolean isDatabaseAccess(UserResourceAccess access) {
        String resourceType = firstNonBlank(access.getResourceType(), "");
        return resourceType.toUpperCase(Locale.ROOT).contains("DATABASE")
                || resourceType.toUpperCase(Locale.ROOT).contains("TABLE")
                || clean(access.getDatabaseName()) != null;
    }

    private boolean isHiveAccess(UserResourceAccess access) {
        String value = firstNonBlank(access.getEngineType(), access.getAuthBackend(), access.getSource(), "");
        String normalized = value.toUpperCase(Locale.ROOT);
        return normalized.contains("HIVE") || normalized.contains("SENTRY") || normalized.contains("RANGER");
    }

    private int collectYarnEvidence(String cluster, List<DgaUser> users) {
        ClusterEndpoint endpoint = endpoint(cluster, ClusterEndpoint.TYPE_YARN);
        int count = 0;
        for (DgaUser user : users) {
            try {
                String url = trimTrailingSlash(endpoint.getUrl()) + "/ws/v1/cluster/apps?user="
                        + urlEncode(user.getUsername()) + "&states=RUNNING,FINISHED,KILLED,FAILED&limit=1";
                ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers(endpoint)), Map.class);
                Map body = response.getBody();
                List apps = nestedList(body, "apps", "app");
                if (apps == null || apps.isEmpty()) {
                    continue;
                }
                Map app = (Map) apps.get(0);
                LocalDateTime active = latest(parseTime(app.get("finishedTime")), parseTime(app.get("startedTime")));
                String evidence = "YARN app " + firstNonBlank(stringValue(app.get("id")), "-")
                        + " state=" + firstNonBlank(stringValue(app.get("state")), "-");
                saveEvidence(user.getUsername(), resolveClusterName(cluster), clusterCode(resolveClusterName(cluster)),
                        "YARN", active, evidence, active == null ? "LOW" : "HIGH",
                        active == null ? "PRESENT" : "OBSERVED", null);
                count++;
            } catch (Exception ignored) {
                // Keep the whole scan useful when a single user has no YARN history.
            }
        }
        return count;
    }

    private int collectHdfsEvidence(String cluster, List<DgaUser> users) {
        ClusterEndpoint endpoint = endpoint(cluster, ClusterEndpoint.TYPE_HDFS);
        int count = 0;
        for (DgaUser user : users) {
            try {
                String url = trimTrailingSlash(endpoint.getUrl()) + "/webhdfs/v1/user/"
                        + urlEncode(user.getUsername()) + "?op=GETFILESTATUS";
                ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers(endpoint)), Map.class);
                Map status = nestedMap(response.getBody(), "FileStatus");
                if (status == null) {
                    continue;
                }
                LocalDateTime active = latest(parseTime(status.get("accessTime")), parseTime(status.get("modificationTime")));
                String evidence = "HDFS home directory /user/" + user.getUsername() + " file status observed";
                saveEvidence(user.getUsername(), resolveClusterName(cluster), clusterCode(resolveClusterName(cluster)),
                        "HDFS", active, evidence, active == null ? "LOW" : "MEDIUM",
                        active == null ? "PRESENT" : "OBSERVED", null);
                count++;
            } catch (Exception ignored) {
                // Missing home directory is a normal outcome and not a scan blocker.
            }
        }
        return count;
    }

    private int collectHueEvidence(String cluster, List<DgaUser> users) {
        ClusterEndpoint endpoint = endpoint(cluster, ClusterEndpoint.TYPE_HUE);
        int count = 0;
        for (DgaUser user : users) {
            try {
                String url = trimTrailingSlash(endpoint.getUrl()) + "/desktop/api/users/" + urlEncode(user.getUsername());
                ResponseEntity<Map> response = restTemplate().exchange(url, HttpMethod.GET,
                        new HttpEntity<>(headers(endpoint)), Map.class);
                Map body = response.getBody();
                LocalDateTime active = latest(parseTime(valueAt(body, "last_login")),
                        parseTime(valueAt(body, "lastLogin")),
                        parseTime(valueAt(body, "user.last_login")));
                if (active == null) {
                    continue;
                }
                saveEvidence(user.getUsername(), resolveClusterName(cluster), clusterCode(resolveClusterName(cluster)),
                        "HUE", active, "Hue user API last_login " + formatTime(active),
                        "HIGH", "OBSERVED", null);
                count++;
            } catch (Exception ignored) {
                // Hue deployments vary; endpoint absence for a user should not fail the whole scan.
            }
        }
        return count;
    }

    private Map<String, List<AccessActivityEvidence>> evidenceByUser(List<DgaUser> users, Set<String> selectedSources, String cluster) {
        Map<String, List<AccessActivityEvidence>> result = new HashMap<>();
        Set<String> usernames = new HashSet<>();
        for (DgaUser user : users) {
            usernames.add(user.getUsername());
        }
        if (usernames.isEmpty()) {
            return result;
        }
        Set<String> querySources = new LinkedHashSet<>(selectedSources);
        querySources.add("DGA");
        for (AccessActivityEvidence evidence : evidenceRepository.findEvidenceForUsers(usernames, querySources, cluster)) {
            List<AccessActivityEvidence> list = result.get(evidence.getUsername());
            if (list == null) {
                list = new ArrayList<>();
                result.put(evidence.getUsername(), list);
            }
            list.add(evidence);
        }
        return result;
    }

    private Boolean scanInactiveUser(DgaUser user, LocalDateTime inactiveCutoff, int inactiveDays,
                                     List<AccessActivityEvidence> evidences) {
        ActivityDecision activity = decideActivity(user, evidences);
        if (activity.lastActiveAt != null) {
            user.setLastActiveAt(activity.lastActiveAt);
            user.setLastActiveSource(activity.lastActiveSource);
            dgaUserRepository.save(user);
        }
        if (activity.lastActiveAt == null || !activity.lastActiveAt.isBefore(inactiveCutoff)) {
            return null;
        }
        AccessGovernanceIssue issue = baseUserIssue(user, "SILENT_ACCOUNT", "MEDIUM",
                "账号超过 " + inactiveDays + " 天未活跃");
        issue.setIssueKey(issueKey("SILENT_ACCOUNT", clusterValue(user.getClusterName()), user.getUsername()));
        issue.setSourceSystems(activity.sourceSystems);
        issue.setLastActiveAt(activity.lastActiveAt);
        issue.setLastActiveSource(activity.lastActiveSource);
        issue.setConfidence(activity.confidence);
        issue.setEvidence("最近活跃时间 " + formatTime(activity.lastActiveAt)
                + "，来源 " + activity.sourceSystems + "，置信度 " + activity.confidence
                + "；" + activity.evidenceSummary);
        issue.setRecommendation(activity.lowConfidence
                ? "没有真实来源活动时间，当前使用 DGA 低置信度兜底；建议补齐 Hue/YARN/HDFS/HiveServer2 等来源后复核。"
                : "确认业务是否仍需要该账号；无人使用则冻结或删除，保留则补充负责人。");
        return upsertIssue(issue);
    }

    private ActivityDecision decideActivity(DgaUser user, List<AccessActivityEvidence> evidences) {
        ActivityDecision decision = new ActivityDecision();
        LinkedHashSet<String> hitSources = new LinkedHashSet<>();
        List<String> evidenceParts = new ArrayList<>();
        if (evidences != null) {
            for (AccessActivityEvidence evidence : evidences) {
                if (evidence.getSourceSystem() != null) {
                    hitSources.add(evidence.getSourceSystem());
                }
                if (evidence.getEvidence() != null && evidenceParts.size() < 4) {
                    evidenceParts.add(evidence.getSourceSystem() + ": " + evidence.getEvidence());
                }
                if (evidence.getLastActiveAt() == null || "DGA".equals(evidence.getSourceSystem())) {
                    continue;
                }
                if (decision.lastActiveAt == null || evidence.getLastActiveAt().isAfter(decision.lastActiveAt)) {
                    decision.lastActiveAt = evidence.getLastActiveAt();
                    decision.lastActiveSource = evidence.getSourceSystem();
                    decision.confidence = firstNonBlank(evidence.getConfidence(), "MEDIUM");
                }
            }
        }
        if (decision.lastActiveAt != null) {
            decision.sourceSystems = hitSources.isEmpty()
                    ? decision.lastActiveSource
                    : String.join(",", hitSources);
            decision.evidenceSummary = evidenceParts.isEmpty()
                    ? "真实来源活动证据"
                    : String.join("；", evidenceParts);
            return decision;
        }

        AccessActivityEvidence dgaEvidence = latestDgaEvidence(evidences);
        LocalDateTime fallback = dgaEvidence == null
                ? firstNonNull(user.getLastActiveAt(), user.getUpdateTime(), user.getCreateTime())
                : dgaEvidence.getLastActiveAt();
        decision.lastActiveAt = fallback;
        decision.lastActiveSource = "DGA";
        hitSources.add("DGA");
        decision.sourceSystems = String.join(",", hitSources);
        decision.confidence = "LOW";
        decision.lowConfidence = true;
        decision.evidenceSummary = dgaEvidence != null
                ? firstNonBlank(dgaEvidence.getEvidence(), "DGA 兜底")
                : "没有任何真实来源活动时间，使用 DGA 用户更新时间/创建时间兜底";
        return decision;
    }

    private AccessActivityEvidence latestDgaEvidence(List<AccessActivityEvidence> evidences) {
        if (evidences == null) {
            return null;
        }
        AccessActivityEvidence latest = null;
        for (AccessActivityEvidence evidence : evidences) {
            if (!"DGA".equals(evidence.getSourceSystem()) || evidence.getLastActiveAt() == null) {
                continue;
            }
            if (latest == null || evidence.getLastActiveAt().isAfter(latest.getLastActiveAt())) {
                latest = evidence;
            }
        }
        return latest;
    }

    private Boolean scanExpiryRequirement(DgaUser user) {
        String userType = normalizeUserType(user.getUserType());
        if (!requiresExpiry(userType)) {
            return null;
        }
        boolean missingExpiry = user.getExpiresAt() == null;
        boolean expired = user.getExpiresAt() != null && user.getExpiresAt().isBefore(LocalDateTime.now());
        if (!missingExpiry && !expired) {
            return null;
        }
        AccessGovernanceIssue issue = baseUserIssue(user, "TEMP_USER_EXPIRY_REQUIRED", "HIGH",
                "外包/临时账号过期时间不合规");
        issue.setIssueKey(issueKey("TEMP_USER_EXPIRY_REQUIRED", clusterValue(user.getClusterName()), user.getUsername()));
        issue.setSourceSystems(identitySource(user));
        issue.setEvidence(missingExpiry
                ? "用户类型为 " + userType + "，但未设置过期时间。"
                : "用户类型为 " + userType + "，过期时间 " + formatTime(user.getExpiresAt()) + " 已到期。");
        issue.setRecommendation("补齐有效过期时间；已到期账号应先确认负责人，再冻结或删除。");
        return upsertIssue(issue);
    }

    private Boolean scanUnusedDatabasePermissions(List<UserResourceAccess> accesses,
                                                  PermissionUsageResult usageResult,
                                                  int inactiveDays) {
        List<UserResourceAccess> unusedAccesses = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (access.getId() == null || !usageResult.coveredAccessIds.contains(access.getId())) {
                continue;
            }
            if (!usageResult.usedByAccessId.containsKey(access.getId())) {
                unusedAccesses.add(access);
            }
        }
        if (unusedAccesses.isEmpty()) {
            return null;
        }
        UserResourceAccess first = unusedAccesses.get(0);
        AccessGovernanceIssue issue = baseAccountAccessIssue(first, "UNUSED_DATABASE_PERMISSION", "MEDIUM",
                "账号存在长期未使用的数据库权限");
        issue.setIssueKey(issueKey("UNUSED_DATABASE_PERMISSION",
                clusterValue(firstNonBlank(first.getClusterCode(), first.getClusterName())), first.getUsername()));
        issue.setSourceSystems(accessSources(unusedAccesses));
        issue.setPermission(joinPermissions(unusedAccesses));
        issue.setLastActiveAt(latestUnusedPermissionUseAt(unusedAccesses, usageResult));
        issue.setConfidence(unusedIssueConfidence(unusedAccesses, usageResult));
        issue.setEvidence(trimTo("该账号有 " + unusedAccesses.size() + " 条数据库权限在最近 "
                + Math.max(inactiveDays, 1) + " 天未被对应引擎审计命中："
                + summarizeUnusedAccesses(unusedAccesses, usageResult, 10) + "。", 1000));
        issue.setRecommendation("先确认业务是否还需要这些数据库权限；不需要则回收，需要保留则补充负责人和复核记录。");
        return upsertIssue(issue);
    }

    private String unusedIssueConfidence(List<UserResourceAccess> accesses, PermissionUsageResult usageResult) {
        for (UserResourceAccess access : accesses) {
            if (access.getId() != null && usageResult.directCoveredAccessIds.contains(access.getId())) {
                return "HIGH";
            }
        }
        return "LOW";
    }

    private LocalDateTime latestUnusedPermissionUseAt(List<UserResourceAccess> accesses, PermissionUsageResult usageResult) {
        LocalDateTime latest = null;
        for (UserResourceAccess access : accesses) {
            LocalDateTime usedAt = lastPermissionUseAt(access, usageResult);
            if (usedAt != null && (latest == null || usedAt.isAfter(latest))) {
                latest = usedAt;
            }
        }
        return latest;
    }

    private String summarizeUnusedAccesses(List<UserResourceAccess> accesses,
                                           PermissionUsageResult usageResult,
                                           int limit) {
        List<String> items = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (items.size() >= limit) {
                items.add("...");
                break;
            }
            String permission = firstNonBlank(access.getPermission(), "-");
            String resource = accessResourceLabel(access);
            LocalDateTime lastUsedAt = lastPermissionUseAt(access, usageResult);
            items.add(permission + " " + resource + " [" + accessSource(access) + "]"
                    + "，" + unusedDurationText(lastUsedAt) + auxiliaryEvidenceText(access, usageResult));
        }
        return String.join("；", items);
    }

    private String auxiliaryEvidenceText(UserResourceAccess access, PermissionUsageResult usageResult) {
        if (access == null || access.getId() == null) {
            return "";
        }
        AuditHit hit = usageResult.auxiliaryByAccessId.get(access.getId());
        if (hit == null) {
            return "";
        }
        String source = firstNonBlank(hit.sourceSystem, "辅助证据");
        String active = hit.lastActiveAt == null ? "无时间" : formatTime(hit.lastActiveAt);
        String sample = clean(hit.sample) == null ? "" : "，" + hit.sample;
        return "；辅助证据 " + source + " 最近活动 " + active + sample;
    }

    private String accessResourceLabel(UserResourceAccess access) {
        String resource = firstNonBlank(access.getDatabaseName(), "-");
        if (clean(access.getTableName()) != null) {
            return resource + "." + access.getTableName().trim();
        }
        if (clean(access.getDatabaseName()) != null) {
            return resource + "/*";
        }
        return resource;
    }

    private String unusedDurationText(LocalDateTime lastUsedAt) {
        if (lastUsedAt == null) {
            return "审计表无历史命中或已超出审计保留期";
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(lastUsedAt, LocalDateTime.now()));
        return "最后使用 " + formatTime(lastUsedAt) + "，已 " + days + " 天未使用";
    }

    private Boolean scanHighPrivilegeAccount(List<UserResourceAccess> accesses, LocalDateTime reviewCutoff, int reviewDays) {
        List<UserResourceAccess> highPrivilegeAccesses = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (!isHighPrivilege(access.getPermission())) {
                continue;
            }
            boolean due = access.getReviewDueAt() == null || access.getReviewDueAt().isBefore(LocalDateTime.now());
            boolean stale = access.getLastReviewedAt() == null || access.getLastReviewedAt().isBefore(reviewCutoff);
            if (due || stale) {
                highPrivilegeAccesses.add(access);
            }
        }
        if (highPrivilegeAccesses.isEmpty()) {
            return null;
        }
        UserResourceAccess first = highPrivilegeAccesses.get(0);
        AccessGovernanceIssue issue = baseAccountAccessIssue(first, "HIGH_PRIVILEGE_REVIEW", "HIGH",
                "账号存在高权限需要定期复核");
        issue.setIssueKey(issueKey("HIGH_PRIVILEGE_REVIEW",
                clusterValue(firstNonBlank(first.getClusterCode(), first.getClusterName())), first.getUsername()));
        issue.setSourceSystems(accessSources(highPrivilegeAccesses));
        issue.setPermission(joinPermissions(highPrivilegeAccesses));
        issue.setEvidence(trimTo("该账号存在 " + highPrivilegeAccesses.size() + " 条高权限："
                + summarizeAccesses(highPrivilegeAccesses, 8) + "；复核周期 " + reviewDays + " 天。", 1000));
        issue.setRecommendation("按账号复核业务必要性；确认保留后更新该账号相关高权限复核时间，不需要则回收权限。");
        return upsertIssue(issue);
    }

    private Boolean scanUnownedAccount(List<UserResourceAccess> accesses) {
        List<UserResourceAccess> unownedAccesses = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (access.getOwner() == null || access.getOwner().trim().isEmpty()) {
                unownedAccesses.add(access);
            }
        }
        if (unownedAccesses.isEmpty()) {
            return null;
        }
        UserResourceAccess first = unownedAccesses.get(0);
        AccessGovernanceIssue issue = baseAccountAccessIssue(first, "UNOWNED_PERMISSION", "MEDIUM",
                "账号存在未认领权限");
        issue.setIssueKey(issueKey("UNOWNED_PERMISSION",
                clusterValue(firstNonBlank(first.getClusterCode(), first.getClusterName())), first.getUsername()));
        issue.setSourceSystems(accessSources(unownedAccesses));
        issue.setPermission(joinPermissions(unownedAccesses));
        issue.setEvidence(trimTo("该账号有 " + unownedAccesses.size() + " 条权限缺少主负责人："
                + summarizeAccesses(unownedAccesses, 10) + "。", 1000));
        issue.setRecommendation("要求业务主负责人按账号认领；保存后会批量回写该账号下缺 owner 的权限记录。");
        return upsertIssue(issue);
    }

    private AccessGovernanceIssue baseUserIssue(DgaUser user, String issueType, String severity, String title) {
        AccessGovernanceIssue issue = new AccessGovernanceIssue();
        issue.setIssueType(issueType);
        issue.setSeverity(severity);
        issue.setUsername(user.getUsername());
        issue.setClusterName(user.getClusterName());
        issue.setClusterCode(clusterCode(user.getClusterName()));
        issue.setEvidence(title);
        return issue;
    }

    private AccessGovernanceIssue baseAccessIssue(UserResourceAccess access, String issueType, String severity, String title) {
        AccessGovernanceIssue issue = new AccessGovernanceIssue();
        issue.setIssueType(issueType);
        issue.setSeverity(severity);
        issue.setUsername(access.getUsername());
        issue.setClusterCode(access.getClusterCode());
        issue.setClusterName(access.getClusterName());
        issue.setResourceType(access.getResourceType());
        issue.setDatabaseName(access.getDatabaseName());
        issue.setTableName(access.getTableName());
        issue.setPermission(access.getPermission());
        issue.setAccessId(access.getId());
        issue.setOwner(access.getOwner());
        issue.setCollaboratorOwners(access.getCollaboratorOwners());
        issue.setEvidence(title);
        return issue;
    }

    private AccessGovernanceIssue baseAccountAccessIssue(UserResourceAccess access, String issueType, String severity, String title) {
        AccessGovernanceIssue issue = new AccessGovernanceIssue();
        issue.setIssueType(issueType);
        issue.setSeverity(severity);
        issue.setUsername(access.getUsername());
        issue.setClusterCode(access.getClusterCode());
        issue.setClusterName(access.getClusterName());
        issue.setResourceType("ACCOUNT");
        issue.setOwner(access.getOwner());
        issue.setCollaboratorOwners(access.getCollaboratorOwners());
        issue.setEvidence(title);
        return issue;
    }

    private boolean upsertIssue(AccessGovernanceIssue issue) {
        List<AccessGovernanceIssue> openIssues = issueRepository.findOpenByIssueKey(issue.getIssueKey());
        if (openIssues == null || openIssues.isEmpty()) {
            issueRepository.save(issue);
            return true;
        }
        AccessGovernanceIssue existing = openIssues.get(0);
        existing.setIssueType(issue.getIssueType());
        existing.setSeverity(issue.getSeverity());
        existing.setUsername(issue.getUsername());
        existing.setClusterCode(issue.getClusterCode());
        existing.setClusterName(issue.getClusterName());
        existing.setResourceType(issue.getResourceType());
        existing.setDatabaseName(issue.getDatabaseName());
        existing.setTableName(issue.getTableName());
        existing.setPermission(issue.getPermission());
        existing.setAccessId(issue.getAccessId());
        existing.setOwner(issue.getOwner());
        existing.setCollaboratorOwners(issue.getCollaboratorOwners());
        existing.setSourceSystems(issue.getSourceSystems());
        existing.setLastActiveAt(issue.getLastActiveAt());
        existing.setLastActiveSource(issue.getLastActiveSource());
        existing.setConfidence(issue.getConfidence());
        existing.setEvidence(issue.getEvidence());
        existing.setRecommendation(issue.getRecommendation());
        issueRepository.save(existing);
        return false;
    }

    private Specification<AccessGovernanceIssue> issueSpec(String cluster,
                                                           String issueTypes,
                                                           String status,
                                                           String username) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String cleanCluster = clean(cluster);
            if (cleanCluster != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("clusterCode"), cleanCluster),
                        cb.equal(root.get("clusterName"), cleanCluster)
                ));
            }
            Set<String> types = normalizeList(issueTypes);
            if (!types.isEmpty()) {
                predicates.add(root.get("issueType").in(types));
            }
            if (clean(status) != null && !"ALL".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (clean(username) != null) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void closeOpenIssue(String issueKey, String operator) {
        List<AccessGovernanceIssue> issues = issueRepository.findOpenByIssueKey(issueKey);
        for (AccessGovernanceIssue issue : issues) {
            issue.setStatus("RESOLVED");
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolvedBy(operator);
            issueRepository.save(issue);
        }
    }

    private void closeLegacyPermissionScopedIssues(String cluster, String operator) {
        for (AccessGovernanceIssue issue : issueRepository.findUnresolvedByCluster(cluster)) {
            if (!isPermissionGovernanceType(issue.getIssueType())) {
                continue;
            }
            String key = issue.getIssueKey();
            if (key == null || key.split("\\|", -1).length != 2) {
                continue;
            }
            issue.setStatus("RESOLVED");
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolvedBy(operator);
            issueRepository.save(issue);
        }
    }

    private void closeUserLifecycleIssues(String cluster, String operator) {
        for (AccessGovernanceIssue issue : issueRepository.findUnresolvedByCluster(cluster)) {
            if (!"SILENT_ACCOUNT".equals(issue.getIssueType())
                    && !"TEMP_USER_EXPIRY_REQUIRED".equals(issue.getIssueType())) {
                continue;
            }
            issue.setStatus("RESOLVED");
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolvedBy(operator);
            issueRepository.save(issue);
        }
    }

    private boolean isPermissionGovernanceType(String issueType) {
        return "HIGH_PRIVILEGE_REVIEW".equals(issueType)
                || "UNOWNED_PERMISSION".equals(issueType)
                || "UNUSED_DATABASE_PERMISSION".equals(issueType);
    }

    private AccessGovernanceIssue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "治理问题不存在: " + issueId));
    }

    private AccessOwner ensureOwner(String ownerCode, String displayName, String email, String source, String operator) {
        if (ownerCode == null || ownerCode.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入负责人账号");
        }
        String code = ownerCode.trim();
        AccessOwner existing = ownerRepository.findByOwnerCodeIgnoreCase(code);
        if (existing == null) {
            existing = new AccessOwner();
            existing.setOwnerCode(code);
            existing.setCreatedBy(operator);
        }
        existing.setDisplayName(firstNonBlank(displayName, existing.getDisplayName(), code));
        existing.setEmail(firstNonBlank(email, existing.getEmail()));
        existing.setSource(firstNonBlank(source, existing.getSource(), "MANUAL"));
        existing.setStatus("ACTIVE");
        return ownerRepository.save(existing);
    }

    private void ensureOwners(List<String> owners, String operator) {
        if (owners == null) {
            return;
        }
        for (String owner : owners) {
            if (owner != null && !owner.trim().isEmpty()) {
                ensureOwner(owner, owner, null, "MANUAL", operator);
            }
        }
    }

    private void applyOwnerToAccountPermissions(AccessGovernanceIssue issue, String owner,
                                                List<String> collaboratorOwners) {
        String collaborators = joinOwners(collaboratorOwners);
        for (UserResourceAccess access : activeAccountAccesses(issue)) {
            if (clean(access.getOwner()) != null) {
                continue;
            }
            access.setOwner(owner);
            access.setCollaboratorOwners(collaborators);
            userResourceAccessRepository.save(access);
        }
    }

    private List<UserResourceAccess> activeAccountAccesses(AccessGovernanceIssue issue) {
        String cluster = firstNonBlank(issue.getClusterCode(), issue.getClusterName());
        List<UserResourceAccess> accesses = userResourceAccessRepository.findActiveByCluster(cluster);
        List<UserResourceAccess> result = new ArrayList<>();
        for (UserResourceAccess access : accesses) {
            if (sameAccount(issue, access)) {
                result.add(access);
            }
        }
        return result;
    }

    private boolean sameAccount(AccessGovernanceIssue issue, UserResourceAccess access) {
        if (!sameText(issue.getUsername(), access.getUsername())) {
            return false;
        }
        String issueCluster = firstNonBlank(issue.getClusterCode(), issue.getClusterName());
        if (issueCluster == null) {
            return true;
        }
        return sameText(issueCluster, access.getClusterCode()) || sameText(issueCluster, access.getClusterName());
    }

    private Map<String, List<UserResourceAccess>> accessByAccount(List<UserResourceAccess> accesses) {
        Map<String, List<UserResourceAccess>> result = new LinkedHashMap<>();
        for (UserResourceAccess access : accesses) {
            String username = clean(access.getUsername());
            if (username == null) {
                continue;
            }
            String cluster = clusterValue(firstNonBlank(access.getClusterCode(), access.getClusterName()));
            String key = cluster.toLowerCase(Locale.ROOT) + "|" + username.toLowerCase(Locale.ROOT);
            List<UserResourceAccess> list = result.get(key);
            if (list == null) {
                list = new ArrayList<>();
                result.put(key, list);
            }
            list.add(access);
        }
        return result;
    }

    private String accountIssueKey(String issueType, List<UserResourceAccess> accesses) {
        if (accesses == null || accesses.isEmpty()) {
            return issueKey(issueType, "-", "-");
        }
        UserResourceAccess first = accesses.get(0);
        return issueKey(issueType, clusterValue(firstNonBlank(first.getClusterCode(), first.getClusterName())),
                first.getUsername());
    }

    private List<User> platformOwnerCandidates(String search) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll(PageRequest.of(0, 20)).getContent();
        }
        return userRepository.findTop20ByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, search);
    }

    private void saveEvidence(String username, String clusterName, String clusterCode, String sourceSystem,
                              LocalDateTime lastActiveAt, String evidence, String confidence, String status, String message) {
        String key = issueKey("ACTIVITY", clusterValue(clusterCode), sourceSystem, username);
        AccessActivityEvidence record = evidenceRepository.findByEvidenceKey(key);
        if (record == null) {
            record = new AccessActivityEvidence();
            record.setEvidenceKey(key);
        }
        record.setUsername(username);
        record.setClusterCode(clusterCode);
        record.setClusterName(clusterName);
        record.setSourceSystem(sourceSystem);
        record.setLastActiveAt(lastActiveAt);
        record.setEvidence(evidence);
        record.setConfidence(confidence);
        record.setStatus(status);
        record.setMessage(message);
        record.setCollectedAt(LocalDateTime.now());
        evidenceRepository.save(record);
    }

    private boolean isSourceConfigured(Cluster cluster, String source) {
        if ("DGA".equals(source)) {
            return true;
        }
        if (cluster == null || cluster.getClusterCode() == null) {
            return false;
        }
        if ("RANGER".equals(source)) {
            return firstEndpoint(cluster, ClusterEndpoint.TYPE_RANGER_DB) != null
                    || firstEndpoint(cluster, ClusterEndpoint.TYPE_RANGER) != null;
        }
        String endpointType = endpointTypeForSource(source);
        if (endpointType == null) {
            return false;
        }
        return !endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                cluster.getClusterCode(), endpointType, "ACTIVE").isEmpty();
    }

    private boolean isSourceConfiguredForAnyCluster(String source) {
        return configuredClusterCount(source) > 0;
    }

    private int configuredClusterCount(String source) {
        if ("DGA".equals(source)) {
            return 1;
        }
        String endpointType = endpointTypeForSource(source);
        if (endpointType == null) {
            return 0;
        }
        int count = 0;
        for (Cluster cluster : clusterRepository.findActiveClusters()) {
            if (cluster == null || cluster.getClusterCode() == null) {
                continue;
            }
            if ("RANGER".equals(source)) {
                if (firstEndpoint(cluster, ClusterEndpoint.TYPE_RANGER_DB) != null
                        || firstEndpoint(cluster, ClusterEndpoint.TYPE_RANGER) != null) {
                    count++;
                }
                continue;
            }
            if (!endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                    cluster.getClusterCode(), endpointType, "ACTIVE").isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private String sourceStatusMessage(Cluster cluster, String source) {
        if ("DGA".equals(source)) {
            return "使用平台登录和 DGA 用户更新时间";
        }
        if (cluster == null) {
            return "请先选择具体集群";
        }
        if (!isSourceConfigured(cluster, source)) {
            return "未接入 " + source + " endpoint";
        }
        if ("HIVE_SERVER2".equals(source)) {
            ClusterEndpoint endpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_HIVE_SERVER2);
            String auditTable = endpoint == null ? null : clean(endpoint.getServiceName());
            return auditTable == null
                    ? "已配置 HiveServer2；如需未使用权限判定，请在端点服务名配置可直接查询的 Hive/Ranger 审计表名"
                    : "已配置，将直接查询 Hive/Ranger 审计表 " + auditTable;
        }
        if ("RANGER".equals(source)) {
            ClusterEndpoint auditEndpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_RANGER_DB);
            if (auditEndpoint != null) {
                String serviceName = rangerServiceName(cluster.getClusterCode(), auditEndpoint);
                return "已配置 Ranger Audit MySQL；将自动识别 x_access_audit/xa_access_audit"
                        + (serviceName == null ? "" : "，repo_name=" + serviceName);
            }
            return "已配置 Ranger policy API；如需长期未使用判定，请新增 RANGER_DB 端点连接 Ranger MySQL 审计库";
        }
        if ("HDFS".equals(source)) {
            ClusterEndpoint endpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_HDFS);
            String auditTable = endpoint == null ? null : clean(endpoint.getServiceName());
            return auditTable == null
                    ? "已配置 HDFS；未设置审计表时仅作为低置信度辅助活动证据"
                    : "已配置，将直接查询 HDFS 审计表/视图 " + auditTable + " 作为辅助证据";
        }
        if ("YARN".equals(source)) {
            ClusterEndpoint endpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_YARN);
            String auditTable = endpoint == null ? null : clean(endpoint.getServiceName());
            return auditTable == null
                    ? "已配置 YARN；将查询 application history 作为低置信度辅助证据"
                    : "已配置，将直接查询 YARN history 表/视图 " + auditTable + " 作为辅助证据";
        }
        if ("STARROCKS".equals(source)) {
            ClusterEndpoint endpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_STARROCKS_JDBC);
            return "已配置，将通过 JDBC 直接查询审计表 " + auditTableName(endpoint, source);
        }
        if ("DORIS".equals(source)) {
            ClusterEndpoint endpoint = firstEndpoint(cluster, ClusterEndpoint.TYPE_DORIS_JDBC);
            return "已配置，将通过 JDBC 直接查询审计表 " + auditTableName(endpoint, source);
        }
        return "已配置";
    }

    private String sourceStatusMessageForAll(String source) {
        if ("DGA".equals(source)) {
            return "全部集群：使用平台登录和 DGA 用户更新时间";
        }
        int count = configuredClusterCount(source);
        if (count <= 0) {
            return "全部集群未接入 " + source + " endpoint";
        }
        if ("HIVE_SERVER2".equals(source)) {
            return "全部集群：已接入 " + count + " 个 HiveServer2 端点；未使用权限判定仍要求端点服务名配置可直接查询的 Hive/Ranger 审计表";
        }
        if ("RANGER".equals(source)) {
            return "全部集群：已接入 " + count + " 个 Ranger/Ranger DB 端点；优先使用 RANGER_DB 读取 x_access_audit/xa_access_audit";
        }
        if ("HDFS".equals(source)) {
            return "全部集群：已接入 " + count + " 个 HDFS 端点；作为 Hive 权限治理低置信度辅助证据";
        }
        if ("YARN".equals(source)) {
            return "全部集群：已接入 " + count + " 个 YARN 端点；作为 Hive 权限治理低置信度辅助证据";
        }
        return "全部集群：已接入 " + count + " 个端点；扫描时按权限所属集群直接查询审计表";
    }

    private ClusterEndpoint firstEndpoint(Cluster cluster, String endpointType) {
        if (cluster == null || cluster.getClusterCode() == null) {
            return null;
        }
        List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                cluster.getClusterCode(), endpointType, "ACTIVE");
        return endpoints == null || endpoints.isEmpty() ? null : endpoints.get(0);
    }

    private ClusterEndpoint rangerAuditEndpoint(String cluster) {
        Cluster resolved = resolveClusterObject(cluster);
        return firstEndpoint(resolved, ClusterEndpoint.TYPE_RANGER_DB);
    }

    private String rangerServiceName(String cluster, ClusterEndpoint auditEndpoint) {
        String configured = auditEndpoint == null ? null : clean(auditEndpoint.getServiceName());
        if (configured != null) {
            return configured;
        }
        Cluster resolved = resolveClusterObject(cluster);
        ClusterEndpoint rangerEndpoint = firstEndpoint(resolved, ClusterEndpoint.TYPE_RANGER);
        return rangerEndpoint == null ? null : clean(rangerEndpoint.getServiceName());
    }

    private ClusterEndpoint endpoint(String cluster, String endpointType) {
        Cluster resolved = resolveClusterObject(cluster);
        if (resolved == null || resolved.getClusterCode() == null) {
            throw new IllegalStateException("请先选择具体集群后再采集 " + endpointType);
        }
        List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                resolved.getClusterCode(), endpointType, "ACTIVE");
        if (endpoints == null || endpoints.isEmpty()) {
            throw new IllegalStateException("集群未配置 ACTIVE " + endpointType + " endpoint");
        }
        return endpoints.get(0);
    }

    private String endpointTypeForSource(String source) {
        if ("LDAP".equals(source)) return ClusterEndpoint.TYPE_LDAP;
        if ("RANGER".equals(source)) return ClusterEndpoint.TYPE_RANGER;
        if ("HIVE_SERVER2".equals(source)) return ClusterEndpoint.TYPE_HIVE_SERVER2;
        if ("STARROCKS".equals(source)) return ClusterEndpoint.TYPE_STARROCKS_JDBC;
        if ("DORIS".equals(source)) return ClusterEndpoint.TYPE_DORIS_JDBC;
        if ("HDFS".equals(source)) return ClusterEndpoint.TYPE_HDFS;
        if ("YARN".equals(source)) return ClusterEndpoint.TYPE_YARN;
        if ("HUE".equals(source)) return ClusterEndpoint.TYPE_HUE;
        return null;
    }

    private String sourceLabel(String source) {
        if ("HIVE_SERVER2".equals(source)) {
            return "HiveServer2";
        }
        if ("RANGER".equals(source)) {
            return "Ranger Audit";
        }
        if ("HDFS".equals(source)) {
            return "HDFS Audit";
        }
        if ("YARN".equals(source)) {
            return "YARN History";
        }
        if ("STARROCKS".equals(source)) {
            return "StarRocks";
        }
        if ("DORIS".equals(source)) {
            return "Doris";
        }
        return source;
    }

    private boolean isHighPrivilege(String permission) {
        if (permission == null) {
            return false;
        }
        for (String part : permission.split(",")) {
            String normalized = part.trim().toUpperCase(Locale.ROOT).replace("_PRIV", "");
            if (HIGH_PRIVILEGE_PERMISSIONS.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String accessSource(UserResourceAccess access) {
        String backend = firstNonBlank(access.getAuthBackend(), access.getSource(), access.getEngineType(), "DGA");
        String normalized = backend.toUpperCase(Locale.ROOT);
        if (normalized.contains("RANGER")) {
            return "RANGER";
        }
        if (normalized.contains("STARROCK")) {
            return "STARROCKS";
        }
        if (normalized.contains("DORIS")) {
            return "DORIS";
        }
        if (normalized.contains("HIVE") || normalized.contains("SENTRY")) {
            return "HIVE_SERVER2";
        }
        return normalized;
    }

    private String accessSources(List<UserResourceAccess> accesses) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        for (UserResourceAccess access : accesses) {
            String source = accessSource(access);
            if (source != null && !source.trim().isEmpty()) {
                sources.add(source);
            }
        }
        return sources.isEmpty() ? "DGA" : String.join(",", sources);
    }

    private String joinPermissions(List<UserResourceAccess> accesses) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (UserResourceAccess access : accesses) {
            String permission = access.getPermission();
            if (permission == null) {
                continue;
            }
            for (String part : permission.split(",")) {
                String normalized = clean(part);
                if (normalized != null) {
                    permissions.add(normalized.toUpperCase(Locale.ROOT));
                }
            }
        }
        return trimTo(permissions.isEmpty() ? null : String.join(",", permissions), 100);
    }

    private String summarizeAccesses(List<UserResourceAccess> accesses, int limit) {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        int max = Math.max(limit, 1);
        for (UserResourceAccess access : accesses) {
            if (items.size() >= max) {
                break;
            }
            String resource = firstNonBlank(access.getDatabaseName(), "-");
            if (clean(access.getTableName()) != null) {
                resource = resource + "." + access.getTableName().trim();
            } else if (clean(access.getDatabaseName()) != null) {
                resource = resource + "/*";
            }
            String permission = firstNonBlank(access.getPermission(), "-");
            items.add(permission + " " + resource + " [" + accessSource(access) + "]");
        }
        if (items.isEmpty()) {
            return "-";
        }
        String summary = String.join("；", items);
        if (accesses.size() > items.size()) {
            summary += "；等 " + (accesses.size() - items.size()) + " 条";
        }
        return trimTo(summary, 900);
    }

    private String identitySource(DgaUser user) {
        String strategy = user.getCreationStrategy() == null ? "" : user.getCreationStrategy().toUpperCase(Locale.ROOT);
        if (strategy.contains("LDAP") || strategy.contains("IPA")) {
            return "LDAP";
        }
        return "DGA";
    }

    private String normalizeUserType(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return "INTERNAL";
        }
        return userType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean requiresExpiry(String userType) {
        return "OUTSOURCER".equals(userType) || "TEMPORARY".equals(userType);
    }

    private Set<String> normalizeSources(String sourceText) {
        Set<String> values = normalizeList(sourceText);
        if (values.isEmpty()) {
            values.addAll(SOURCE_SYSTEMS);
        }
        values.retainAll(new HashSet<>(SOURCE_SYSTEMS));
        return values;
    }

    private Set<String> normalizeList(String text) {
        Set<String> values = new LinkedHashSet<>();
        if (text == null) {
            return values;
        }
        for (String part : text.split(",")) {
            String value = part.trim().toUpperCase(Locale.ROOT);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String resolveClusterName(String clusterIdentifier) {
        Cluster cluster = resolveClusterObject(clusterIdentifier);
        if (cluster != null && cluster.getClusterName() != null) {
            return cluster.getClusterName();
        }
        String cleaned = clean(clusterIdentifier);
        return cleaned;
    }

    private Cluster resolveClusterObject(String clusterIdentifier) {
        String cleaned = clean(clusterIdentifier);
        if (cleaned == null) {
            return null;
        }
        Cluster cluster = clusterRepository.findByClusterCode(cleaned);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(cleaned);
        }
        return cluster;
    }

    private boolean isAllClusterScope(String clusterIdentifier) {
        String cleaned = clean(clusterIdentifier);
        return cleaned == null
                || "ALL".equalsIgnoreCase(cleaned)
                || "全部集群".equals(cleaned);
    }

    private String clusterCode(String clusterNameOrCode) {
        String cleaned = clean(clusterNameOrCode);
        if (cleaned == null) {
            return null;
        }
        Cluster cluster = clusterRepository.findByClusterName(cleaned);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterCode(cleaned);
        }
        return cluster != null && cluster.getClusterCode() != null ? cluster.getClusterCode() : cleaned;
    }

    private String issueKey(String type, String... parts) {
        StringBuilder builder = new StringBuilder(type);
        for (String part : parts) {
            builder.append("|").append(part == null ? "-" : part.trim());
        }
        return builder.toString();
    }

    private Map<String, Object> ownerMap(AccessOwner owner) {
        return ownerMap(owner.getOwnerCode(), firstNonBlank(owner.getDisplayName(), owner.getOwnerCode()),
                owner.getEmail(), owner.getSource());
    }

    private Map<String, Object> ownerMap(String code, String displayName, String email, String source) {
        Map<String, Object> item = new HashMap<>();
        item.put("ownerCode", code);
        item.put("displayName", displayName);
        item.put("email", email);
        item.put("source", source);
        item.put("label", displayName == null || displayName.equals(code) ? code : displayName + " (" + code + ")");
        return item;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    private HttpHeaders headers(ClusterEndpoint endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json,*/*");
        if (endpoint != null && endpoint.getUsername() != null && !endpoint.getUsername().trim().isEmpty()) {
            String token = endpoint.getUsername() + ":" + (endpoint.getPassword() == null ? "" : endpoint.getPassword());
            String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return headers;
    }

    private List nestedList(Map body, String parent, String child) {
        Map parentMap = nestedMap(body, parent);
        if (parentMap == null) {
            return null;
        }
        Object value = parentMap.get(child);
        return value instanceof List ? (List) value : null;
    }

    private Map nestedMap(Map body, String key) {
        if (body == null || key == null) {
            return null;
        }
        Object value = body.get(key);
        return value instanceof Map ? (Map) value : null;
    }

    private Object valueAt(Map body, String path) {
        if (body == null || path == null) {
            return null;
        }
        Object current = body;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map) current).get(part);
        }
        return current;
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof Number) {
            long millis = ((Number) value).longValue();
            if (millis <= 0) {
                return null;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            if (text.matches("\\d{13}")) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZoneId.systemDefault());
            }
            if (text.matches("\\d{10}")) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(text)), ZoneId.systemDefault());
            }
            if (text.matches("\\d{14}(\\.\\d+)?Z")) {
                return LocalDateTime.ofInstant(Instant.from(LDAP_TIME_FORMATTER.parse(text)), ZoneId.systemDefault());
            }
            String normalized = text.replace("Z", "");
            if (normalized.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")) {
                normalized = normalized.replace(' ', 'T');
            }
            return LocalDateTime.parse(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime latest = null;
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) {
                latest = value;
            }
        }
        return latest;
    }

    private LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private LocalDateTime parseWindowsFileTime(Object value) {
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        try {
            long fileTime = Long.parseLong(text);
            if (fileTime <= 116444736000000000L) {
                return parseTime(text);
            }
            long millis = (fileTime / 10000L) - 11644473600000L;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        } catch (Exception e) {
            return parseTime(text);
        }
    }

    private String joinOwners(List<String> owners) {
        if (owners == null || owners.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String owner : owners) {
            if (owner != null && !owner.trim().isEmpty()) {
                cleaned.add(owner.trim());
            }
        }
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    private String clusterValue(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private void increment(Map<String, Integer> target, String key) {
        String normalized = firstNonBlank(key, "UNKNOWN");
        Integer current = target.get(normalized);
        target.put(normalized, current == null ? 1 : current + 1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "未复核" : time.format(TIME_FORMATTER);
    }

    private String formatOptionalTime(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String stringValue(Object value) {
        return value == null || value.toString().trim().isEmpty() ? null : value.toString().trim();
    }

    private long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private String readableMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String trimTrailingSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "";
        }
    }

    private String quotedTableName(String tableName) {
        String value = clean(tableName);
        if (value == null || !value.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){0,2}")) {
            throw new IllegalArgumentException("审计表名不合法: " + tableName);
        }
        List<String> parts = new ArrayList<>();
        for (String part : value.split("\\.")) {
            parts.add(quotedIdentifier(part));
        }
        return String.join(".", parts);
    }

    private String quotedIdentifier(String identifier) {
        String value = clean(identifier);
        if (value == null || !value.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("字段名不合法: " + identifier);
        }
        return "`" + value.replace("`", "``") + "`";
    }

    private static class ActivityDecision {
        private LocalDateTime lastActiveAt;
        private String lastActiveSource;
        private String sourceSystems;
        private String confidence = "LOW";
        private String evidenceSummary = "";
        private boolean lowConfidence;
    }

    private static class AuditTableSpec {
        private final String tableName;
        private final String timeColumn;
        private final String userColumn;
        private final String dbColumn;
        private final String stateColumn;
        private final String queryIdColumn;
        private final String stmtColumn;

        private AuditTableSpec(String tableName, String timeColumn, String userColumn, String dbColumn,
                               String stateColumn, String queryIdColumn, String stmtColumn) {
            this.tableName = tableName;
            this.timeColumn = timeColumn;
            this.userColumn = userColumn;
            this.dbColumn = dbColumn;
            this.stateColumn = stateColumn;
            this.queryIdColumn = queryIdColumn;
            this.stmtColumn = stmtColumn;
        }
    }

    private static class SqlAuditCollection {
        private final Set<String> candidateUsers;
        private final Map<String, AuditHit> hitsByUser;
        private final String tableName;

        private SqlAuditCollection(Set<String> candidateUsers, Map<String, AuditHit> hitsByUser, String tableName) {
            this.candidateUsers = candidateUsers;
            this.hitsByUser = hitsByUser;
            this.tableName = tableName;
        }
    }

    private static class AuditHit {
        private String username;
        private String databaseName;
        private LocalDateTime lastActiveAt;
        private long hitCount;
        private long selectCount;
        private long insertCount;
        private long createCount;
        private long alterCount;
        private long dropCount;
        private LocalDateTime selectLastAt;
        private LocalDateTime insertLastAt;
        private LocalDateTime createLastAt;
        private LocalDateTime alterLastAt;
        private LocalDateTime dropLastAt;
        private String sample;
        private String sourceSystem;
        private String confidence;
    }

    private static class PermissionUsageResult {
        private final Set<Long> coveredAccessIds = new HashSet<>();
        private final Set<Long> directCoveredAccessIds = new HashSet<>();
        private final Map<Long, AuditHit> usedByAccessId = new HashMap<>();
        private final Map<Long, AuditHit> historyByAccessId = new HashMap<>();
        private final Map<Long, AuditHit> auxiliaryByAccessId = new HashMap<>();
    }
}
