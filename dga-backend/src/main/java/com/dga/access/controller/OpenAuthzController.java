package com.dga.access.controller;

import com.dga.access.dto.openapi.AuthzClusterItem;
import com.dga.access.dto.openapi.AuthzGrantItem;
import com.dga.access.dto.openapi.AuthzGrantRequest;
import com.dga.access.dto.openapi.AuthzGrantView;
import com.dga.access.dto.openapi.AuthzNamedItem;
import com.dga.access.dto.openapi.AuthzPagedResponse;
import com.dga.access.dto.openapi.AuthzPermissionSnapshot;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.security.CurrentUser;
import com.dga.access.service.AdminGuard;
import com.dga.access.service.authorization.AuthorizationCapability;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.access.service.authorization.AuthorizationSupport;
import com.dga.access.service.authorization.GrantCommand;
import com.dga.access.service.authorization.RevokeCommand;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.repository.ClusterRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/openapi/v1/authz")
@Tag(name = "开放授权接口", description = "对外提供集群、用户、资源、权限查询以及批量授权回收能力")
@SecurityRequirement(name = "bearerAuth")
public class OpenAuthzController {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping("/clusters")
    @Operation(summary = "查询可授权集群")
    public List<AuthzClusterItem> listClusters() {
        List<Cluster> clusters = clusterRepository.findActiveClusters();
        List<AuthzClusterItem> items = new ArrayList<>();
        for (Cluster cluster : clusters) {
            String clusterIdentifier = cluster.getClusterCode() != null && !cluster.getClusterCode().trim().isEmpty()
                    ? cluster.getClusterCode() : cluster.getClusterName();
            AuthorizationCapability capability = authorizationService.capability(clusterIdentifier);
            AuthzClusterItem item = new AuthzClusterItem();
            item.setClusterCode(cluster.getClusterCode());
            item.setClusterName(cluster.getClusterName());
            item.setEngineType(capability.getEngineType());
            item.setAuthBackend(capability.getAuthBackend());
            item.setStatus(capability.getStatus());
            items.add(item);
        }
        items.sort(Comparator.comparing(AuthzClusterItem::getClusterCode, Comparator.nullsLast(String::compareToIgnoreCase)));
        return items;
    }

    @GetMapping("/principals")
    @Operation(summary = "查询用户列表")
    public AuthzPagedResponse<AuthzNamedItem> listPrincipals(
            @Parameter(description = "集群编码") @RequestParam String clusterCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<String> principals;
        try {
            principals = authorizationService.listPrincipals(clusterCode);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
        return paginateNames(principals, keyword, page, pageSize);
    }

    @GetMapping("/resources/databases")
    @Operation(summary = "查询库列表")
    public AuthzPagedResponse<AuthzNamedItem> listDatabases(
            @RequestParam String clusterCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        List<String> databases;
        try {
            databases = authorizationService.listDatabases(clusterCode);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
        List<String> filtered = databases == null ? Collections.emptyList() : databases.stream()
                .filter(item -> !isInternalDatabase(item))
                .collect(Collectors.toList());
        return paginateNames(filtered, keyword, page, pageSize);
    }

    @GetMapping("/resources/tables")
    @Operation(summary = "查询表列表")
    public AuthzPagedResponse<AuthzNamedItem> listTables(
            @RequestParam String clusterCode,
            @RequestParam String database,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        List<String> tables;
        try {
            tables = authorizationService.listTables(clusterCode, database);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
        return paginateNames(tables, keyword, page, pageSize);
    }

    @GetMapping("/permissions")
    @Operation(summary = "查询用户当前权限")
    public AuthzPermissionSnapshot listPermissions(
            @RequestParam String clusterCode,
            @RequestParam String username) {
        try {
            List<Map<String, Object>> rawPermissions = authorizationService.getUserPermissions(username, clusterCode);
            AuthzPermissionSnapshot snapshot = new AuthzPermissionSnapshot();
            snapshot.setClusterCode(authorizationService.resolveClusterCodeOrName(clusterCode));
            snapshot.setUsername(username);
            snapshot.setEngineType(authorizationService.engineType(clusterCode));
            snapshot.setAuthBackend(authorizationService.authBackend(clusterCode));
            snapshot.setGrants(normalizePermissionRows(rawPermissions));
            return snapshot;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
    }

    @PostMapping("/grants")
    @Operation(summary = "批量授权")
    public Map<String, Object> grant(@RequestBody AuthzGrantRequest request, HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可调用开放授权写接口");
        validateGrantRequest(request);
        String operator = CurrentUser.usernameOrUnknown();
        int processed = 0;
        try {
            for (AuthzGrantItem item : request.getGrants()) {
                GrantCommand command = buildGrantCommand(request.getUsername(), request.getClusterCode(), item);
                authorizationService.grant(command);
                saveResourceAccess(request.getUsername(), request.getClusterCode(), item, operator, "OPENAPI");
                processed++;
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("clusterCode", request.getClusterCode());
        response.put("username", request.getUsername());
        response.put("processed", processed);
        return response;
    }

    @PostMapping("/revokes")
    @Operation(summary = "批量回收")
    public Map<String, Object> revoke(@RequestBody AuthzGrantRequest request, HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可调用开放授权写接口");
        validateGrantRequest(request);
        String operator = CurrentUser.usernameOrUnknown();
        int processed = 0;
        try {
            for (AuthzGrantItem item : request.getGrants()) {
                RevokeCommand command = buildRevokeCommand(request.getUsername(), request.getClusterCode(), item);
                authorizationService.revoke(command);
                revokeResourceAccess(request.getUsername(), request.getClusterCode(), item, operator, "OPENAPI");
                processed++;
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, readableAuthorizationError(e), e);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("clusterCode", request.getClusterCode());
        response.put("username", request.getUsername());
        response.put("processed", processed);
        return response;
    }

    private AuthzPagedResponse<AuthzNamedItem> paginateNames(List<String> source, String keyword, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 200);
        List<String> filtered = source == null ? Collections.emptyList() : source.stream()
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, filtered.size());

        AuthzPagedResponse<AuthzNamedItem> response = new AuthzPagedResponse<>();
        response.setPage(normalizedPage);
        response.setPageSize(normalizedPageSize);
        response.setTotal(filtered.size());
        response.setItems(filtered.subList(fromIndex, toIndex).stream()
                .map(AuthzNamedItem::new)
                .collect(Collectors.toList()));
        return response;
    }

    private boolean matchesKeyword(String item, String keyword) {
        if (item == null) {
            return false;
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        return item.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isInternalDatabase(String database) {
        if (database == null) {
            return false;
        }
        String normalized = database.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("information_schema")
                || normalized.startsWith("sys")
                || normalized.startsWith("mysql")
                || normalized.startsWith("performance_schema");
    }

    private void validateGrantRequest(AuthzGrantRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        if (isBlank(request.getClusterCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clusterCode 不能为空");
        }
        if (isBlank(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username 不能为空");
        }
        if (request.getGrants() == null || request.getGrants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grants 不能为空");
        }
        for (AuthzGrantItem item : request.getGrants()) {
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grants 中存在空项");
            }
            if (isBlank(item.getPermission())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permission 不能为空");
            }
            AuthorizationSupport.validatePermission(item.getPermission());
            String resourceType = normalizeResourceType(item.getResourceType(), item.getTableName());
            if (!"DATABASE".equals(resourceType) && !"TABLE".equals(resourceType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resourceType 仅支持 DATABASE 或 TABLE");
            }
            if (isBlank(item.getDatabaseName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "databaseName 不能为空");
            }
            if ("TABLE".equals(resourceType) && isBlank(item.getTableName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TABLE 级授权必须提供 tableName");
            }
            item.setResourceType(resourceType);
            item.setPermission(item.getPermission().trim().toUpperCase(Locale.ROOT));
            item.setDatabaseName(item.getDatabaseName().trim());
            item.setTableName(normalizeAuthTable(item.getTableName()));
        }
    }

    private String normalizeResourceType(String resourceType, String tableName) {
        if (!isBlank(resourceType)) {
            return resourceType.trim().toUpperCase(Locale.ROOT);
        }
        return isBlank(tableName) ? "DATABASE" : "TABLE";
    }

    private GrantCommand buildGrantCommand(String username, String clusterCode, AuthzGrantItem item) {
        GrantCommand command = new GrantCommand();
        command.setUsername(username);
        command.setCluster(clusterCode);
        command.setDatabase(item.getDatabaseName());
        command.setTable(normalizeAuthTable(item.getTableName()));
        command.setPermission(item.getPermission());
        return command;
    }

    private RevokeCommand buildRevokeCommand(String username, String clusterCode, AuthzGrantItem item) {
        RevokeCommand command = new RevokeCommand();
        command.setUsername(username);
        command.setCluster(clusterCode);
        command.setDatabase(item.getDatabaseName());
        command.setTable(normalizeAuthTable(item.getTableName()));
        command.setPermission(item.getPermission());
        return command;
    }

    private void saveResourceAccess(String username, String clusterCode, AuthzGrantItem item, String operator, String source) {
        String clusterIdentifier = clusterCode != null && !clusterCode.isEmpty() ? clusterCode : "CDH-Cluster-01";
        String normalizedTable = normalizeAuthTable(item.getTableName());
        UserResourceAccess access = new UserResourceAccess();
        access.setUsername(username);
        access.setClusterName(clusterIdentifier);
        access.setClusterCode(authorizationService.resolveClusterCodeOrName(clusterIdentifier));
        access.setEngineType(authorizationService.engineType(clusterIdentifier));
        access.setAuthBackend(authorizationService.authBackend(clusterIdentifier));
        access.setResourceType(normalizedTable == null ? "DATABASE" : "TABLE");
        access.setDatabaseName(item.getDatabaseName());
        access.setTableName(normalizedTable);
        access.setPermission(item.getPermission());
        access.setGrantedBy(operator);
        access.setSource(source);
        access.setStatus("ACTIVE");
        access.setDeleted(false);
        access.setGrantTime(LocalDateTime.now());
        userResourceAccessRepository.save(access);
    }

    private void revokeResourceAccess(String username, String clusterCode, AuthzGrantItem item, String operator, String source) {
        String clusterIdentifier = clusterCode != null && !clusterCode.isEmpty() ? clusterCode : "CDH-Cluster-01";
        String normalizedTable = normalizeAuthTable(item.getTableName());
        int updated;
        if (normalizedTable == null) {
            updated = userResourceAccessRepository.softDeleteDatabaseAccess(
                    username, clusterIdentifier, item.getDatabaseName(), item.getPermission(), operator);
        } else {
            updated = userResourceAccessRepository.softDeleteTableAccess(
                    username, clusterIdentifier, item.getDatabaseName(), normalizedTable, item.getPermission(), operator);
        }
        if (updated == 0) {
            UserResourceAccess access = new UserResourceAccess();
            access.setUsername(username);
            access.setClusterName(clusterIdentifier);
            access.setClusterCode(authorizationService.resolveClusterCodeOrName(clusterIdentifier));
            access.setEngineType(authorizationService.engineType(clusterIdentifier));
            access.setAuthBackend(authorizationService.authBackend(clusterIdentifier));
            access.setResourceType(normalizedTable == null ? "DATABASE" : "TABLE");
            access.setDatabaseName(item.getDatabaseName());
            access.setTableName(normalizedTable);
            access.setPermission(item.getPermission());
            access.setGrantedBy(operator);
            access.setRevokedBy(operator);
            access.setSource(source);
            access.setStatus("REVOKED");
            access.setDeleted(true);
            access.setRevokeTime(LocalDateTime.now());
            userResourceAccessRepository.save(access);
        }
    }

    private List<AuthzGrantView> normalizePermissionRows(List<Map<String, Object>> rawPermissions) {
        List<AuthzGrantView> grants = new ArrayList<>();
        if (rawPermissions == null) {
            return grants;
        }
        for (Map<String, Object> row : rawPermissions) {
            grants.add(normalizePermissionRow(row));
        }
        return grants;
    }

    private AuthzGrantView normalizePermissionRow(Map<String, Object> row) {
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

        if (grantText != null && grantText.toUpperCase(Locale.ROOT).startsWith("GRANT ")) {
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

        AuthzGrantView grant = new AuthzGrantView();
        grant.setResourceType(resourceType);
        grant.setDatabaseName(database);
        grant.setTableName(table);
        grant.setPermission(permission != null ? permission.toUpperCase(Locale.ROOT) : "-");
        grant.setGrantText(grantText);
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

    private String readableAuthorizationError(Exception e) {
        String message = e.getMessage() == null ? "授权执行失败" : e.getMessage();
        if (message.contains("Access denied") || message.toLowerCase(Locale.ROOT).contains("denied")) {
            return "授权端点账号权限不足: " + message;
        }
        if (message.toLowerCase(Locale.ROOT).contains("syntax") || message.toLowerCase(Locale.ROOT).contains("sql")) {
            return "授权 SQL 执行失败: " + message;
        }
        return message;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
