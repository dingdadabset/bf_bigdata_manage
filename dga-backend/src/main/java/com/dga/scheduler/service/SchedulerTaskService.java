package com.dga.scheduler.service;

import com.dga.access.security.CurrentUser;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.lineage.entity.DataLineage;
import com.dga.lineage.repository.DataLineageRepository;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.TableMetadataRepository;
import com.dga.scheduler.entity.SchedulerTaskContext;
import com.dga.scheduler.entity.SchedulerTaskOwner;
import com.dga.scheduler.repository.SchedulerTaskContextRepository;
import com.dga.scheduler.repository.SchedulerTaskOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerTaskService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int AZKABAN_RECENT_EXECUTION_LIMIT = 5000;
    private static final long SNAPSHOT_TTL_MILLIS = 30_000L;
    private static final int RISK_WINDOW_DAYS = 7;
    private static final long START_DELAY_THRESHOLD_MILLIS = 30 * 60 * 1000L;

    private final Map<String, SnapshotCacheEntry> snapshotCache = new ConcurrentHashMap<>();
    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private SchedulerTaskOwnerRepository ownerRepository;

    @Autowired
    private DataLineageRepository lineageRepository;

    @Autowired
    private TableMetadataRepository tableRepository;

    @Autowired
    private SchedulerTaskContextRepository taskContextRepository;

    public Map<String, Object> overview(String clusterCode, Long endpointId) {
        SchedulerSnapshot snapshot = loadSnapshot(clusterCode, endpointId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterCode", snapshot.endpoint.getClusterCode());
        result.put("endpointId", snapshot.endpoint.getId());
        result.put("sourceType", snapshot.endpoint.getEndpointType());
        result.put("endpointName", endpointName(snapshot.endpoint));
        result.put("totalTasks", snapshot.tasks.size());
        result.put("totalProjects", snapshot.projectNames.size());
        result.put("totalFlows", snapshot.flowNames.size());
        result.put("successTasks", countByHealth(snapshot.tasks, "HEALTHY"));
        result.put("failedTasks", countByHealth(snapshot.tasks, "FAILED"));
        result.put("runningTasks", countByHealth(snapshot.tasks, "RUNNING"));
        result.put("unknownTasks", countByHealth(snapshot.tasks, "UNKNOWN"));
        result.put("ownedTasks", snapshot.tasks.stream().filter(t -> !isBlank(stringValue(t.get("owner")))).count());
        result.put("unownedTasks", snapshot.tasks.stream().filter(t -> isBlank(stringValue(t.get("owner")))).count());
        result.put("activeAssetTasks", snapshot.tasks.stream().filter(t -> Boolean.TRUE.equals(t.get("assetActive"))).count());
        result.put("inactiveAssetTasks", snapshot.tasks.stream().filter(t -> Boolean.FALSE.equals(t.get("assetActive"))).count());
        result.put("recentRuns", snapshot.recentRuns);
        result.put("dailyTrend", snapshot.dailyTrend);
        result.put("generatedAt", snapshot.generatedAt);
        return result;
    }

    public List<Map<String, Object>> tasks(String clusterCode, Long endpointId, String projectName, String flowName,
                                           String owner, String health, Boolean assetActive, String keyword) {
        SchedulerSnapshot snapshot = loadSnapshot(clusterCode, endpointId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> task : snapshot.tasks) {
            if (!matches(projectName, task.get("projectName"))) continue;
            if (!matches(flowName, task.get("flowName"))) continue;
            if (!matches(owner, task.get("owner"))) continue;
            if (!matches(health, task.get("healthStatus"))) continue;
            if (assetActive != null && !assetActive.equals(task.get("assetActive"))) continue;
            if (!matchesKeyword(keyword, task)) continue;
            result.add(task);
        }
        result.sort((a, b) -> compareNullable((LocalDateTime) b.get("lastStartTime"), (LocalDateTime) a.get("lastStartTime")));
        return result;
    }

    public List<Map<String, Object>> riskTasks(String clusterCode, Long endpointId, String day) {
        ClusterEndpoint endpoint = resolveEndpoint(clusterCode, endpointId);
        Map<String, SchedulerTaskOwner> owners = ownerMap(endpoint);
        if (!ClusterEndpoint.TYPE_AZKABAN_DB.equals(endpoint.getEndpointType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "当前仅支持 AZKABAN_DB 调度元数据端点，DOLPHINSCHEDULER_DB 后续接入");
        }
        try (Connection connection = openConnection(endpoint)) {
            return loadAzkabanRiskTasks(connection, endpoint, owners, trimToNull(day));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "读取 Azkaban 风险任务失败: " + readableMessage(e), e);
        }
    }

    public List<Map<String, Object>> groups(String clusterCode, Long endpointId) {
        SchedulerSnapshot snapshot = loadSnapshot(clusterCode, endpointId);
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        for (Map<String, Object> task : snapshot.tasks) {
            String projectName = stringValue(task.get("projectName"));
            String flowName = stringValue(task.get("flowName"));
            String key = projectName + "\n" + flowName;
            Map<String, Object> group = groups.get(key);
            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("clusterCode", snapshot.endpoint.getClusterCode());
                group.put("endpointId", snapshot.endpoint.getId());
                group.put("sourceType", snapshot.endpoint.getEndpointType());
                group.put("projectName", projectName);
                group.put("flowName", flowName);
                group.put("taskCount", 0);
                group.put("failedTasks", 0);
                group.put("assetCount", 0);
                group.put("activeAssetCount", 0);
                group.put("owners", new LinkedHashSet<String>());
                group.put("assets", new ArrayList<Map<String, Object>>());
                groups.put(key, group);
            }
            group.put("taskCount", ((Integer) group.get("taskCount")) + 1);
            if ("FAILED".equals(task.get("healthStatus"))) {
                group.put("failedTasks", ((Integer) group.get("failedTasks")) + 1);
            }
            if (Boolean.TRUE.equals(task.get("assetActive"))) {
                group.put("activeAssetCount", ((Integer) group.get("activeAssetCount")) + 1);
            }
            String taskOwner = stringValue(task.get("owner"));
            if (!isBlank(taskOwner)) {
                @SuppressWarnings("unchecked")
                Set<String> owners = (Set<String>) group.get("owners");
                owners.add(taskOwner);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assets = (List<Map<String, Object>>) group.get("assets");
            appendUniqueAssets(assets, assetsForContext(snapshot.endpoint, projectName, flowName));
            group.put("assetCount", assets.size());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> group : groups.values()) {
            @SuppressWarnings("unchecked")
            Set<String> owners = (Set<String>) group.get("owners");
            group.put("owners", new ArrayList<>(owners));
            result.add(group);
        }
        return result;
    }

    public SchedulerTaskOwner assignOwner(Map<String, Object> request) {
        String clusterCode = requiredString(request, "clusterCode");
        Long endpointId = requiredLong(request, "endpointId");
        String projectName = requiredString(request, "projectName");
        String flowName = requiredString(request, "flowName");
        String taskName = requiredString(request, "taskName");
        ClusterEndpoint endpoint = resolveEndpoint(clusterCode, endpointId);
        SchedulerTaskOwner owner = ownerRepository
                .findByClusterCodeAndSourceEndpointIdAndProjectNameAndFlowNameAndTaskName(
                        endpoint.getClusterCode(), endpoint.getId(), projectName, flowName, taskName)
                .orElseGet(SchedulerTaskOwner::new);
        owner.setClusterCode(endpoint.getClusterCode());
        owner.setSourceEndpointId(endpoint.getId());
        owner.setSourceType(endpoint.getEndpointType());
        owner.setProjectName(projectName);
        owner.setFlowName(flowName);
        owner.setTaskName(taskName);
        owner.setOwner(trimToNull(stringValue(request.get("owner"))));
        owner.setCollaboratorOwners(trimToNull(stringValue(request.get("collaboratorOwners"))));
        owner.setOwnerSource("MANUAL");
        owner.setStatus("ACTIVE");
        owner.setRemark(trimToNull(stringValue(request.get("remark"))));
        owner.setUpdatedBy(CurrentUser.usernameOrUnknown());
        SchedulerTaskOwner saved = ownerRepository.save(owner);
        expireSnapshot(endpoint.getClusterCode(), endpoint.getId());
        return saved;
    }

    private SchedulerSnapshot loadSnapshot(String clusterCode, Long endpointId) {
        ClusterEndpoint endpoint = resolveEndpoint(clusterCode, endpointId);
        String cacheKey = snapshotCacheKey(endpoint.getClusterCode(), endpoint.getId());
        SnapshotCacheEntry cached = snapshotCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.createdAtMillis) < SNAPSHOT_TTL_MILLIS) {
            return cached.snapshot;
        }
        Map<String, SchedulerTaskOwner> owners = ownerMap(endpoint);
        List<Map<String, Object>> tasks = new ArrayList<>();
        List<Map<String, Object>> recentRuns = new ArrayList<>();
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        Set<String> projects = new LinkedHashSet<>();
        Set<String> flows = new LinkedHashSet<>();
        if (ClusterEndpoint.TYPE_AZKABAN_DB.equals(endpoint.getEndpointType())) {
            try (Connection connection = openConnection(endpoint)) {
                tasks.addAll(loadAzkabanTasks(connection, endpoint, owners));
                recentRuns.addAll(loadAzkabanRecentRuns(connection));
                dailyTrend.addAll(loadAzkabanDailyTrend(connection));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "读取 Azkaban 元数据失败: " + readableMessage(e), e);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "当前仅支持 AZKABAN_DB 调度元数据端点，DOLPHINSCHEDULER_DB 后续接入");
        }
        for (Map<String, Object> task : tasks) {
            projects.add(stringValue(task.get("projectName")));
            flows.add(stringValue(task.get("projectName")) + "/" + stringValue(task.get("flowName")));
            enrichAssets(endpoint, task);
        }
        SchedulerSnapshot snapshot = new SchedulerSnapshot();
        snapshot.endpoint = endpoint;
        snapshot.tasks = tasks;
        snapshot.recentRuns = recentRuns;
        snapshot.dailyTrend = dailyTrend;
        snapshot.projectNames = projects;
        snapshot.flowNames = flows;
        snapshot.generatedAt = LocalDateTime.now();
        snapshotCache.put(cacheKey, new SnapshotCacheEntry(snapshot, now));
        return snapshot;
    }

    private List<Map<String, Object>> loadAzkabanTasks(Connection connection, ClusterEndpoint endpoint,
                                                       Map<String, SchedulerTaskOwner> owners) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String projectOwnerExpr = azkabanProjectOwnerExpr(connection);
        long recentWindowStart = System.currentTimeMillis() - (RISK_WINDOW_DAYS * 24L * 60L * 60L * 1000L);
        Map<String, List<Map<String, Object>>> taskDailyRiskMap = loadAzkabanTaskDailyRisk(connection);
        String sql = "SELECT p.name AS project_name, " + projectOwnerExpr + " AS project_owner, ej.flow_id, ej.job_id, " +
                "MAX(rf.start_time) AS last_start_time, MAX(rf.end_time) AS last_end_time, " +
                "SUBSTRING_INDEX(GROUP_CONCAT(ej.status ORDER BY rf.start_time DESC SEPARATOR ','), ',', 1) AS last_status, " +
                "COUNT(*) AS run_count, " +
                "SUM(CASE WHEN rf.start_time >= " + recentWindowStart + " AND ej.status = 70 THEN 1 ELSE 0 END) AS failed_runs_7d, " +
                "SUM(CASE WHEN rf.start_time >= " + recentWindowStart + " THEN 1 ELSE 0 END) AS run_count_7d, " +
                "SUM(CASE WHEN rf.start_time >= " + recentWindowStart + " AND rf.submit_time > 0 AND rf.start_time > rf.submit_time " +
                "AND (rf.start_time - rf.submit_time) >= " + START_DELAY_THRESHOLD_MILLIS + " THEN 1 ELSE 0 END) AS delayed_runs_7d " +
                "FROM (" +
                "  SELECT exec_id, project_id, flow_id, submit_time, start_time, end_time " +
                "  FROM execution_flows " +
                "  ORDER BY start_time DESC " +
                "  LIMIT " + AZKABAN_RECENT_EXECUTION_LIMIT +
                ") rf " +
                "JOIN execution_jobs ej ON ej.exec_id = rf.exec_id " +
                "JOIN projects p ON p.id = rf.project_id " +
                "GROUP BY p.name, " + projectOwnerExpr + ", ej.flow_id, ej.job_id " +
                "ORDER BY last_start_time DESC LIMIT 1000";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String projectName = valueOrUnknown(rs.getString("project_name"));
                String flowName = valueOrUnknown(rs.getString("flow_id"));
                String taskName = valueOrUnknown(rs.getString("job_id"));
                SchedulerTaskOwner assigned = owners.get(ownerKey(projectName, flowName, taskName));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("clusterCode", endpoint.getClusterCode());
                item.put("endpointId", endpoint.getId());
                item.put("sourceType", endpoint.getEndpointType());
                item.put("projectName", projectName);
                item.put("flowName", flowName);
                item.put("taskName", taskName);
                item.put("taskKey", endpoint.getClusterCode() + ":" + endpoint.getId() + ":" + projectName + ":" + flowName + ":" + taskName);
                item.put("owner", assigned != null ? assigned.getOwner() : trimToNull(rs.getString("project_owner")));
                item.put("ownerSource", assigned != null ? assigned.getOwnerSource() : "AZKABAN_PROJECT");
                item.put("collaboratorOwners", assigned != null ? assigned.getCollaboratorOwners() : null);
                item.put("lastStatus", azkabanStatusLabel(rs.getString("last_status")));
                item.put("healthStatus", healthStatus(rs.getString("last_status")));
                item.put("lastStartTime", fromAzkabanTime(rs.getLong("last_start_time")));
                item.put("lastEndTime", fromAzkabanTime(rs.getLong("last_end_time")));
                item.put("runCount", rs.getLong("run_count"));
                item.put("failedRuns7d", rs.getLong("failed_runs_7d"));
                item.put("runCount7d", rs.getLong("run_count_7d"));
                item.put("delayedRuns7d", rs.getLong("delayed_runs_7d"));
                item.put("riskDaily", taskDailyRiskMap.getOrDefault(ownerKey(projectName, flowName, taskName), Collections.emptyList()));
                result.add(item);
            }
        }
        return result;
    }

    private List<Map<String, Object>> loadAzkabanRecentRuns(Connection connection) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String submitUserExpr = columnExists(connection, null, "execution_flows", "submit_user")
                ? "ef.submit_user"
                : "NULL";
        String sql = "SELECT p.name AS project_name, ef.flow_id, ef.exec_id, ef.status, " + submitUserExpr + " AS submit_user, " +
                "ef.start_time, ef.end_time FROM execution_flows ef JOIN projects p ON p.id = ef.project_id " +
                "ORDER BY ef.start_time DESC LIMIT 20";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("projectName", rs.getString("project_name"));
                item.put("flowName", rs.getString("flow_id"));
                item.put("runId", rs.getString("exec_id"));
                item.put("status", azkabanStatusLabel(rs.getString("status")));
                item.put("submitUser", rs.getString("submit_user"));
                item.put("startTime", fromAzkabanTime(rs.getLong("start_time")));
                item.put("endTime", fromAzkabanTime(rs.getLong("end_time")));
                item.put("durationMs", durationMillis(rs.getLong("start_time"), rs.getLong("end_time")));
                result.add(item);
            }
        }
        return result;
    }

    private List<Map<String, Object>> loadAzkabanDailyTrend(Connection connection) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        long recentWindowStart = System.currentTimeMillis() - (RISK_WINDOW_DAYS * 24L * 60L * 60L * 1000L);
        String sql = "SELECT DATE(FROM_UNIXTIME(start_time / 1000)) AS run_day, " +
                "SUM(CASE WHEN status = 50 THEN 1 ELSE 0 END) AS success_count, " +
                "SUM(CASE WHEN status = 70 THEN 1 ELSE 0 END) AS failed_count " +
                "FROM execution_flows " +
                "WHERE start_time >= " + recentWindowStart + " " +
                "GROUP BY DATE(FROM_UNIXTIME(start_time / 1000)) " +
                "ORDER BY run_day ASC";
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = RISK_WINDOW_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.toString());
            item.put("successCount", 0);
            item.put("failedCount", 0);
            byDay.put(day.toString(), item);
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String day = rs.getString("run_day");
                Map<String, Object> item = byDay.get(day);
                if (item == null) {
                    item = new LinkedHashMap<>();
                    item.put("day", day);
                    byDay.put(day, item);
                }
                item.put("successCount", rs.getLong("success_count"));
                item.put("failedCount", rs.getLong("failed_count"));
            }
        }
        result.addAll(byDay.values());
        return result;
    }

    private List<Map<String, Object>> loadAzkabanRiskTasks(Connection connection, ClusterEndpoint endpoint,
                                                           Map<String, SchedulerTaskOwner> owners, String day)
            throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String projectOwnerExpr = azkabanProjectOwnerExpr(connection);
        String selectedDay = trimToNull(day);
        boolean singleDay = selectedDay != null;
        long fromMillis = !singleDay
                ? System.currentTimeMillis() - (RISK_WINDOW_DAYS * 24L * 60L * 60L * 1000L)
                : LocalDate.parse(selectedDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toMillis = !singleDay
                ? Long.MAX_VALUE
                : LocalDate.parse(selectedDay).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String upperBound = toMillis == Long.MAX_VALUE ? "" : " AND ef.start_time < " + toMillis + " ";
        String riskFilter = singleDay
                ? "WHERE failed_count >= 1 OR delayed_count >= 1 "
                : "WHERE failed_count >= 2 OR delayed_count >= 2 ";
        int limit = singleDay ? 500 : 200;
        String sql = "SELECT * FROM (" +
                "SELECT p.name AS project_name, " + projectOwnerExpr + " AS project_owner, " +
                "SUBSTRING_INDEX(ej.flow_id, ',', 1) AS root_flow_id, ej.flow_id AS raw_flow_id, ej.job_id, " +
                "MAX(ef.start_time) AS last_start_time, MAX(ef.end_time) AS last_end_time, " +
                "SUM(CASE WHEN ej.status = 70 THEN 1 ELSE 0 END) AS failed_count, " +
                "SUM(CASE WHEN ef.submit_time > 0 AND ef.start_time > ef.submit_time " +
                "AND (ef.start_time - ef.submit_time) >= " + START_DELAY_THRESHOLD_MILLIS + " THEN 1 ELSE 0 END) AS delayed_count, " +
                "COUNT(*) AS run_count " +
                "FROM execution_flows ef " +
                "JOIN execution_jobs ej ON ej.exec_id = ef.exec_id " +
                "JOIN projects p ON p.id = ef.project_id " +
                "WHERE ef.start_time >= " + fromMillis + " " + upperBound +
                "GROUP BY p.name, " + projectOwnerExpr + ", SUBSTRING_INDEX(ej.flow_id, ',', 1), ej.flow_id, ej.job_id " +
                ") risk_rows " +
                riskFilter +
                "ORDER BY (failed_count * 2 + delayed_count) DESC, last_start_time DESC " +
                "LIMIT " + limit;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String projectName = valueOrUnknown(rs.getString("project_name"));
                String rootFlowName = valueOrUnknown(rs.getString("root_flow_id"));
                String rawFlowName = valueOrUnknown(rs.getString("raw_flow_id"));
                String taskName = valueOrUnknown(rs.getString("job_id"));
                SchedulerTaskOwner assigned = resolveOwner(owners, projectName, rawFlowName, rootFlowName, taskName);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("clusterCode", endpoint.getClusterCode());
                item.put("endpointId", endpoint.getId());
                item.put("sourceType", endpoint.getEndpointType());
                item.put("projectName", projectName);
                item.put("flowName", rootFlowName);
                item.put("rawFlowName", rawFlowName);
                item.put("taskName", taskName);
                item.put("taskKey", endpoint.getClusterCode() + ":" + endpoint.getId() + ":" + projectName + ":" + rawFlowName + ":" + taskName);
                item.put("owner", assigned != null ? assigned.getOwner() : trimToNull(rs.getString("project_owner")));
                item.put("ownerSource", assigned != null ? assigned.getOwnerSource() : "AZKABAN_PROJECT");
                item.put("collaboratorOwners", assigned != null ? assigned.getCollaboratorOwners() : null);
                item.put("failedCount", rs.getLong("failed_count"));
                item.put("delayedCount", rs.getLong("delayed_count"));
                item.put("runCount", rs.getLong("run_count"));
                item.put("lastStartTime", fromAzkabanTime(rs.getLong("last_start_time")));
                item.put("lastEndTime", fromAzkabanTime(rs.getLong("last_end_time")));
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, List<Map<String, Object>>> loadAzkabanTaskDailyRisk(Connection connection) throws SQLException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        long recentWindowStart = System.currentTimeMillis() - (RISK_WINDOW_DAYS * 24L * 60L * 60L * 1000L);
        String sql = "SELECT p.name AS project_name, ej.flow_id, ej.job_id, DATE(FROM_UNIXTIME(rf.start_time / 1000)) AS run_day, " +
                "SUM(CASE WHEN ej.status = 70 THEN 1 ELSE 0 END) AS failed_count, " +
                "SUM(CASE WHEN rf.submit_time > 0 AND rf.start_time > rf.submit_time " +
                "AND (rf.start_time - rf.submit_time) >= " + START_DELAY_THRESHOLD_MILLIS + " THEN 1 ELSE 0 END) AS delayed_count, " +
                "COUNT(*) AS run_count " +
                "FROM (" +
                "  SELECT exec_id, project_id, flow_id, submit_time, start_time " +
                "  FROM execution_flows " +
                "  WHERE start_time >= " + recentWindowStart + " " +
                "  ORDER BY start_time DESC " +
                "  LIMIT " + AZKABAN_RECENT_EXECUTION_LIMIT +
                ") rf " +
                "JOIN execution_jobs ej ON ej.exec_id = rf.exec_id " +
                "JOIN projects p ON p.id = rf.project_id " +
                "GROUP BY p.name, ej.flow_id, ej.job_id, DATE(FROM_UNIXTIME(rf.start_time / 1000)) " +
                "ORDER BY run_day ASC";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String key = ownerKey(
                        valueOrUnknown(rs.getString("project_name")),
                        valueOrUnknown(rs.getString("flow_id")),
                        valueOrUnknown(rs.getString("job_id"))
                );
                List<Map<String, Object>> rows = result.computeIfAbsent(key, k -> new ArrayList<>());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("day", rs.getString("run_day"));
                item.put("failedCount", rs.getLong("failed_count"));
                item.put("delayedCount", rs.getLong("delayed_count"));
                item.put("runCount", rs.getLong("run_count"));
                rows.add(item);
            }
        }
        return result;
    }

    private void enrichAssets(ClusterEndpoint endpoint, Map<String, Object> task) {
        List<Map<String, Object>> assets = assetsForContext(endpoint,
                stringValue(task.get("projectName")), stringValue(task.get("flowName")), stringValue(task.get("taskName")));
        task.put("assetCount", assets.size());
        task.put("activeAssetCount", assets.stream().filter(a -> Boolean.TRUE.equals(a.get("active"))).count());
        task.put("assetActive", assets.isEmpty() ? null : assets.stream().anyMatch(a -> Boolean.TRUE.equals(a.get("active"))));
        task.put("assets", assets);
    }

    private List<Map<String, Object>> assetsForContext(ClusterEndpoint endpoint, String projectName, String flowName) {
        return assetsForContext(endpoint, projectName, flowName, null);
    }

    private List<Map<String, Object>> assetsForContext(ClusterEndpoint endpoint, String projectName, String flowName, String taskName) {
        List<DataLineage> lineages = lineageRepository.findActiveBySchedulerContext(
                endpoint.getClusterCode(), endpoint.getId(), projectName, flowName);
        Set<Long> tableIds = new LinkedHashSet<>();
        for (DataLineage lineage : lineages) {
            tableIds.add(lineage.getSourceTableId());
            tableIds.add(lineage.getTargetTableId());
        }
        List<SchedulerTaskContext> contexts = taskContextRepository.findActiveBySchedulerContext(
                endpoint.getClusterCode(), endpoint.getId(), projectName, flowName, taskName);
        for (SchedulerTaskContext context : contexts) {
            addMatchedTableIds(tableIds, context.getMatchedTableIds());
        }
        List<Map<String, Object>> assets = new ArrayList<>();
        for (Long tableId : tableIds) {
            if (tableId == null) continue;
            Optional<TableMetadata> table = tableRepository.findById(tableId);
            if (!table.isPresent()) continue;
            TableMetadata t = table.get();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("name", t.getDbName() + "." + t.getTableName());
            item.put("owner", firstNonBlank(t.getOwner(), t.getSourceOwner()));
            item.put("lastAccessTime", t.getLastAccessTime());
            item.put("lastModifyTime", t.getUpdatedAt());
            item.put("lifecycleStatus", t.getLifecycleStatus());
            item.put("active", isAssetActive(t));
            assets.add(item);
        }
        return assets;
    }

    private void addMatchedTableIds(Set<Long> tableIds, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        for (String part : value.split(",")) {
            try {
                tableIds.add(Long.valueOf(part.trim()));
            } catch (Exception ignored) {
            }
        }
    }

    private String azkabanProjectOwnerExpr(Connection connection) throws SQLException {
        if (columnExists(connection, null, "projects", "created_by")) {
            return "p.created_by";
        }
        if (columnExists(connection, null, "projects", "creator")) {
            return "p.creator";
        }
        if (columnExists(connection, null, "projects", "modified_by")) {
            return "p.modified_by";
        }
        if (columnExists(connection, null, "projects", "last_modified_by")) {
            return "p.last_modified_by";
        }
        return "NULL";
    }

    private boolean columnExists(Connection connection, String schemaPattern, String tableName, String columnName)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), schemaPattern, tableName, columnName)) {
            return rs.next();
        }
    }

    private void appendUniqueAssets(List<Map<String, Object>> target, List<Map<String, Object>> candidates) {
        Set<Object> existing = new HashSet<>();
        for (Map<String, Object> item : target) {
            existing.add(item.get("id"));
        }
        for (Map<String, Object> item : candidates) {
            if (existing.add(item.get("id"))) {
                target.add(item);
            }
        }
    }

    private boolean isAssetActive(TableMetadata table) {
        if ("OFFLINE".equalsIgnoreCase(stringValue(table.getLifecycleStatus()))
                || "DEPRECATED".equalsIgnoreCase(stringValue(table.getLifecycleStatus()))) {
            return false;
        }
        LocalDateTime activity = table.getLastAccessTime() != null ? table.getLastAccessTime() : table.getUpdatedAt();
        return activity != null && activity.isAfter(LocalDateTime.now().minusDays(90));
    }

    private Connection openConnection(ClusterEndpoint endpoint) throws SQLException {
        return DriverManager.getConnection(endpoint.getUrl(), endpoint.getUsername(), endpoint.getPassword());
    }

    private ClusterEndpoint resolveEndpoint(String clusterCode, Long endpointId) {
        if (endpointId != null) {
            ClusterEndpoint endpoint = endpointRepository.findById(endpointId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调度端点不存在: " + endpointId));
            if (!isBlank(clusterCode) && !clusterCode.equals(endpoint.getClusterCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调度端点不属于集群: " + clusterCode);
            }
            return endpoint;
        }
        if (isBlank(clusterCode)) {
            List<ClusterEndpoint> endpoints = endpointRepository.findByEndpointTypeAndStatus(ClusterEndpoint.TYPE_AZKABAN_DB, STATUS_ACTIVE);
            if (endpoints.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未配置 ACTIVE AZKABAN_DB 调度端点");
            }
            return endpoints.get(0);
        }
        List<ClusterEndpoint> endpoints = endpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                clusterCode, ClusterEndpoint.TYPE_AZKABAN_DB, STATUS_ACTIVE);
        if (endpoints.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "集群未配置 ACTIVE AZKABAN_DB 调度端点: " + clusterCode);
        }
        return endpoints.get(0);
    }

    private Map<String, SchedulerTaskOwner> ownerMap(ClusterEndpoint endpoint) {
        Map<String, SchedulerTaskOwner> map = new HashMap<>();
        for (SchedulerTaskOwner owner : ownerRepository.findByClusterCodeAndSourceEndpointIdAndStatus(
                endpoint.getClusterCode(), endpoint.getId(), STATUS_ACTIVE)) {
            map.put(ownerKey(owner.getProjectName(), owner.getFlowName(), owner.getTaskName()), owner);
        }
        return map;
    }

    private SchedulerTaskOwner resolveOwner(Map<String, SchedulerTaskOwner> owners, String projectName,
                                            String rawFlowName, String rootFlowName, String taskName) {
        SchedulerTaskOwner direct = owners.get(ownerKey(projectName, rawFlowName, taskName));
        if (direct != null) return direct;
        return owners.get(ownerKey(projectName, rootFlowName, taskName));
    }

    private String healthStatus(String status) {
        String value = azkabanStatusLabel(status).toUpperCase(Locale.ROOT);
        if (value.contains("SUCCEEDED") || value.contains("SUCCESS")) return "HEALTHY";
        if (value.contains("FAILED") || value.contains("KILLED") || value.contains("CANCELLED")) return "FAILED";
        if (value.contains("RUNNING") || value.contains("QUEUED") || value.contains("PREPARING")) return "RUNNING";
        return "UNKNOWN";
    }

    private String azkabanStatusLabel(String status) {
        String value = stringValue(status).trim();
        if (value.matches("\\d+")) {
            switch (Integer.parseInt(value)) {
                case 10:
                    return "READY";
                case 20:
                    return "PREPARING";
                case 30:
                    return "RUNNING";
                case 40:
                    return "PAUSED";
                case 50:
                    return "SUCCEEDED";
                case 60:
                    return "KILLED";
                case 70:
                    return "FAILED";
                default:
                    return "UNKNOWN";
            }
        }
        return value;
    }

    private long countByHealth(List<Map<String, Object>> tasks, String health) {
        return tasks.stream().filter(t -> health.equals(t.get("healthStatus"))).count();
    }

    private LocalDateTime fromAzkabanTime(long value) {
        if (value <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

    private Long durationMillis(long start, long end) {
        if (start <= 0 || end <= 0 || end < start) {
            return null;
        }
        return end - start;
    }

    private String endpointName(ClusterEndpoint endpoint) {
        return firstNonBlank(endpoint.getServiceName(), endpoint.getDescription(), endpoint.getUrl(), endpoint.getEndpointType());
    }

    private String snapshotCacheKey(String clusterCode, Long endpointId) {
        return stringValue(clusterCode) + ":" + String.valueOf(endpointId);
    }

    private void expireSnapshot(String clusterCode, Long endpointId) {
        snapshotCache.remove(snapshotCacheKey(clusterCode, endpointId));
    }

    private String ownerKey(String projectName, String flowName, String taskName) {
        return stringValue(projectName) + "\n" + stringValue(flowName) + "\n" + stringValue(taskName);
    }

    private boolean matches(String expected, Object actual) {
        return isBlank(expected) || expected.equals(stringValue(actual));
    }

    private boolean matchesKeyword(String keyword, Map<String, Object> task) {
        if (isBlank(keyword)) return true;
        String needle = keyword.toLowerCase(Locale.ROOT);
        return stringValue(task.get("projectName")).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(task.get("flowName")).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(task.get("taskName")).toLowerCase(Locale.ROOT).contains(needle)
                || stringValue(task.get("owner")).toLowerCase(Locale.ROOT).contains(needle);
    }

    private int compareNullable(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        return left.compareTo(right);
    }

    private String requiredString(Map<String, Object> request, String key) {
        String value = trimToNull(stringValue(request.get(key)));
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少参数: " + key);
        }
        return value;
    }

    private Long requiredLong(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(stringValue(value));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少参数: " + key);
        }
    }

    private String readableMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return null;
    }

    private String valueOrUnknown(String value) {
        return isBlank(value) ? "UNKNOWN" : value.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class SchedulerSnapshot {
        ClusterEndpoint endpoint;
        List<Map<String, Object>> tasks;
        List<Map<String, Object>> recentRuns;
        List<Map<String, Object>> dailyTrend;
        Set<String> projectNames;
        Set<String> flowNames;
        LocalDateTime generatedAt;
    }

    private static class SnapshotCacheEntry {
        final SchedulerSnapshot snapshot;
        final long createdAtMillis;

        private SnapshotCacheEntry(SchedulerSnapshot snapshot, long createdAtMillis) {
            this.snapshot = snapshot;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
