package com.dga.metadata.controller;

import com.dga.access.service.AdminGuard;
import com.dga.access.security.CurrentUser;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.service.HiveServer2ConnectionService;
import com.dga.lineage.service.LineageGraphService;
import com.dga.metadata.entity.ColumnMetadata;
import com.dga.metadata.entity.DataTheme;
import com.dga.metadata.entity.GovernanceTask;
import com.dga.metadata.entity.MetadataTag;
import com.dga.metadata.entity.MetricDefinition;
import com.dga.metadata.entity.MetadataCollectionTask;
import com.dga.metadata.entity.PartitionMetadata;
import com.dga.metadata.entity.TableBusinessMetadata;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.entity.TableTagMapping;
import com.dga.metadata.repository.ColumnMetadataRepository;
import com.dga.metadata.repository.DataThemeRepository;
import com.dga.metadata.repository.GovernanceTaskRepository;
import com.dga.metadata.repository.MetadataTagRepository;
import com.dga.metadata.repository.MetricDefinitionRepository;
import com.dga.metadata.repository.TableMetadataRepository;
import com.dga.metadata.repository.PartitionMetadataRepository;
import com.dga.metadata.repository.TableBusinessMetadataRepository;
import com.dga.metadata.repository.TableTagMappingRepository;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.datasource.service.DataSourceSyncService;
import com.dga.metadata.service.MetadataCollector;
import com.dga.metadata.service.MetadataCollectionAsyncRunner;
import com.dga.metadata.service.MetadataCollectionResult;
import com.dga.metadata.service.MetadataCollectionService;
import com.dga.metadata.service.MetadataCollectorFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.persistence.criteria.Predicate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metadata")
@Tag(name = "元数据管理", description = "Hive 表资产检索、详情查询、权限查看与采集任务")
@SecurityRequirement(name = "bearerAuth")
public class MetadataController {
    private static final Set<String> SYSTEM_HIVE_DATABASES = new LinkedHashSet<>(
            Arrays.asList("information_schema", "sys", "mysql", "performance_schema", "_statistics_"));

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private PartitionMetadataRepository partitionMetadataRepository;

    @Autowired
    private GovernanceTaskRepository governanceTaskRepository;

    @Autowired
    private DataThemeRepository dataThemeRepository;

    @Autowired
    private TableBusinessMetadataRepository tableBusinessRepository;

    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;

    @Autowired
    private MetadataTagRepository metadataTagRepository;

    @Autowired
    private TableTagMappingRepository tableTagMappingRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private LineageGraphService lineageGraphService;

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private MetadataCollectorFactory collectorFactory;

    @Autowired
    private MetadataCollectionService collectionService;

    @Autowired
    private MetadataCollectionAsyncRunner asyncRunner;

    @Autowired
    private DataSourceSyncService dataSourceSyncService;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private ClusterEndpointRepository clusterEndpointRepository;

    @Autowired
    private HiveServer2ConnectionService hiveServer2ConnectionService;

    @GetMapping("/stats")
    @Operation(summary = "查询元数据统计", description = "返回纳管 Hive 表资产的数量、大小、评分与今日同步统计。")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long tableCount = tableMetadataRepository.countManagedHiveTables();
        Long totalSize = tableMetadataRepository.sumManagedHiveTotalSize();
        Double avgScore = tableMetadataRepository.avgManagedHiveGovernanceScore();
        long todaySyncCount = tableMetadataRepository.countManagedHiveBySyncTimeAfter(LocalDateTime.now().with(LocalTime.MIN));
        long databaseCount = tableMetadataRepository.countDistinctManagedHiveDbName();

        stats.put("tableCount", tableCount);
        stats.put("databaseCount", databaseCount);
        stats.put("totalSize", totalSize != null ? totalSize : 0L);
        stats.put("avgScore", avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0);
        stats.put("todaySyncCount", todaySyncCount);
        
        return stats;
    }

    @GetMapping("/tables")
    public List<TableMetadata> getTables(@RequestParam(required = false) Long dataSourceId) {
        List<TableMetadata> tables;
        if (dataSourceId != null) {
            tables = tableMetadataRepository.findByDataSourceId(dataSourceId);
        } else {
            tables = tableMetadataRepository.findAll();
        }
        return tables.stream().filter(this::isUserHiveTable).collect(Collectors.toList());
    }

    @GetMapping("/tables/page")
    public Page<TableMetadata> getTablesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false) String dbName,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) Long themeId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) String keyword) {
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        Pageable pageable = PageRequest.of(page, size);

        if ((keyword != null && !keyword.trim().isEmpty()) || themeId != null || tagId != null) {
            return tableMetadataRepository.searchAssets(keyword == null ? null : keyword.trim(), dataSourceId, dbName, owner, themeId, tagId, lifecycleStatus, pageable);
        }

        return tableMetadataRepository.findAll((Specification<TableMetadata>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (dataSourceId != null) {
                predicates.add(cb.equal(root.get("dataSourceId"), dataSourceId));
            }
            predicates.add(cb.not(cb.lower(root.get("dbName")).in(SYSTEM_HIVE_DATABASES)));
            if (dbName != null && !dbName.isEmpty()) {
                predicates.add(cb.equal(root.get("dbName"), dbName));
            }
            if (owner != null && !owner.isEmpty()) {
                predicates.add(cb.equal(root.get("owner"), owner));
            }
            if (lifecycleStatus != null && !lifecycleStatus.isEmpty()) {
                if ("ONLINE".equals(lifecycleStatus)) {
                    predicates.add(cb.or(cb.equal(root.get("lifecycleStatus"), lifecycleStatus), cb.isNull(root.get("lifecycleStatus"))));
                } else {
                    predicates.add(cb.equal(root.get("lifecycleStatus"), lifecycleStatus));
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                String likePattern = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("tableName"), likePattern),
                    cb.like(root.get("dbName"), likePattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索表资产", description = "按关键词、数据源、数据库、负责人等条件分页搜索 Hive 表资产。")
    public Page<TableMetadata> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false) String dbName,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) Long themeId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        return tableMetadataRepository.searchAssets(keyword, dataSourceId, dbName, owner, themeId, tagId, lifecycleStatus, PageRequest.of(page, size));
    }

    @GetMapping("/catalog/tree")
    @Operation(summary = "查询技术资产目录", description = "按集群、数据源、数据库构建技术资产目录树。")
    public List<Map<String, Object>> catalogTree(@RequestParam(required = false) String clusterCode,
                                                 @RequestParam(required = false) Long dataSourceId) {
        List<TableMetadata> tables;
        if (dataSourceId != null) {
            tables = tableMetadataRepository.findByDataSourceId(dataSourceId);
        } else if (clusterCode != null && !clusterCode.trim().isEmpty()) {
            tables = tableMetadataRepository.findByClusterCode(clusterCode.trim());
        } else {
            tables = tableMetadataRepository.findAll();
        }

        Map<Long, DataSourceConfig> dataSourceMap = dataSourceRepository.findAllById(
                tables.stream().map(TableMetadata::getDataSourceId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(DataSourceConfig::getId, item -> item));

        Map<String, Map<String, Object>> clusters = new LinkedHashMap<>();
        for (TableMetadata table : tables) {
            DataSourceConfig ds = dataSourceMap.get(table.getDataSourceId());
            if (!isManagedHiveDataSource(ds)) {
                continue;
            }
            if (!isUserHiveTable(table)) {
                continue;
            }
            String resolvedClusterCode = firstNonBlank(table.getClusterCode(), ds.getClusterCode());
            if (resolvedClusterCode.isEmpty()) {
                continue;
            }
            String clusterTitle = ds != null && ds.getClusterName() != null ? ds.getClusterName() : resolvedClusterCode;
            Map<String, Object> clusterNode = clusters.computeIfAbsent(resolvedClusterCode,
                    key -> node("cluster-" + key, clusterTitle, "cluster"));

            String dsKey = "ds-" + table.getDataSourceId();
            Map<String, Object> dsNode = child(clusterNode, dsKey,
                    ds == null ? "数据源 " + table.getDataSourceId() : ds.getName(), "datasource");
            dsNode.put("dataSourceId", table.getDataSourceId());

            String dbKey = dsKey + "-db-" + table.getDbName();
            Map<String, Object> dbNode = child(dsNode, dbKey, table.getDbName(), "database");
            dbNode.put("dataSourceId", table.getDataSourceId());
            dbNode.put("dbName", table.getDbName());

            Map<String, Object> tableNode = node("table-" + table.getId(), table.getTableName(), "table");
            tableNode.put("tableId", table.getId());
            tableNode.put("dataSourceId", table.getDataSourceId());
            tableNode.put("dbName", table.getDbName());
            children(dbNode).add(tableNode);
        }
        return new ArrayList<>(clusters.values());
    }

    @GetMapping("/table/{id}")
    @Operation(summary = "查询表详情", description = "按表 ID 查询 Hive 表资产详情。")
    public TableMetadata getTable(@PathVariable Long id) {
        return tableMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
    }

    @GetMapping("/table/{id}/columns")
    @Operation(summary = "查询字段列表", description = "按表 ID 查询字段元数据列表。")
    public List<ColumnMetadata> getColumns(@PathVariable Long id) {
        return columnMetadataRepository.findByTableId(id);
    }

    @GetMapping("/table/{id}/create-ddl")
    public Map<String, Object> getCreateDdl(@PathVariable Long id) {
        TableMetadata table = getTableOrThrow(id);
        ClusterEndpoint endpoint = resolveHiveServer2Endpoint(table);
        String showCreateSql = "SHOW CREATE TABLE " + quoteHiveIdentifier(table.getDbName()) + "." + quoteHiveIdentifier(table.getTableName());
        List<String> ddlLines = new ArrayList<>();
        try {
            try (Connection connection = hiveServer2ConnectionService.openConnection(endpoint);
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(showCreateSql)) {
                while (rs.next()) {
                    ddlLines.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HIVE_SERVER2 查询失败: " + readableMessage(e), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "HIVE_SERVER2 查询失败: " + readableMessage(e), e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "HIVE_SERVER2");
        result.put("clusterCode", endpoint.getClusterCode());
        result.put("endpointType", endpoint.getEndpointType());
        result.put("sql", String.join("\n", ddlLines));
        return result;
    }

    @GetMapping("/table/{id}/partitions")
    @Operation(summary = "查询分区信息", description = "按表 ID 查询分区总数和最近采集的分区明细。")
    public Map<String, Object> getPartitions(@PathVariable Long id,
                                             @RequestParam(defaultValue = "10") int limit) {
        TableMetadata table = getTableOrThrow(id);
        int displayLimit = Math.max(1, Math.min(limit, 50));
        long partitionCount = table.getPartitionCount() == null ? 0L : table.getPartitionCount();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableId", id);
        result.put("partitionCount", partitionCount);
        result.put("displayLimit", displayLimit);
        result.put("items", partitionMetadataRepository.findByTableIdOrderByLastModifyTimeDescIdDesc(id, PageRequest.of(0, displayLimit)));
        return result;
    }

    @GetMapping("/table/{id}/lineage")
    @Operation(summary = "查询表级血缘", description = "按表 ID 查询聚合后的表级血缘图数据。")
    public Map<String, Object> getLineage(@PathVariable Long id,
                                          @Parameter(description = "调度源类型，可选 AZKABAN_DB / DOLPHINSCHEDULER_DB") @RequestParam(required = false) String sourceType,
                                          @Parameter(description = "调度源端点 ID") @RequestParam(required = false) Long sourceEndpointId) {
        return lineageGraphService.getLineageGraph(id, sourceType, sourceEndpointId);
    }

    @GetMapping("/table/{id}/business")
    public TableBusinessMetadata getBusiness(@PathVariable Long id) {
        getTableOrThrow(id);
        return tableBusinessRepository.findByTableId(id).orElseGet(() -> {
            TableBusinessMetadata business = new TableBusinessMetadata();
            business.setTableId(id);
            return business;
        });
    }

    @PutMapping("/table/{id}/business")
    public TableBusinessMetadata updateBusiness(@PathVariable Long id,
                                                @RequestBody Map<String, Object> requestBody,
                                                HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护业务元数据");
        getTableOrThrow(id);
        TableBusinessMetadata business = tableBusinessRepository.findByTableId(id).orElseGet(() -> {
            TableBusinessMetadata item = new TableBusinessMetadata();
            item.setTableId(id);
            return item;
        });
        business.setThemeId(asLong(requestBody.get("themeId")));
        business.setBusinessDescription(clean(asString(requestBody.get("businessDescription"))));
        business.setBusinessDefinition(clean(asString(requestBody.get("businessDefinition"))));
        business.setBusinessOwner(clean(asString(requestBody.get("businessOwner"))));
        business.setUpdatedBy(currentUsername(request));
        return tableBusinessRepository.save(business);
    }

    @GetMapping("/table/{id}/management")
    public Map<String, Object> getManagement(@PathVariable Long id) {
        TableMetadata table = getTableOrThrow(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", table);
        result.put("tags", tagsForTable(id));
        return result;
    }

    @PutMapping("/table/{id}/management")
    public Map<String, Object> updateManagement(@PathVariable Long id,
                                                @RequestBody Map<String, Object> requestBody,
                                                HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护管理元数据");
        TableMetadata table = getTableOrThrow(id);
        String lifecycleStatus = clean(asString(requestBody.get("lifecycleStatus")));
        if (lifecycleStatus != null && !lifecycleStatus.isEmpty()) {
            table.setLifecycleStatus(lifecycleStatus);
        }
        String owner = clean(asString(requestBody.get("owner")));
        if (requestBody.containsKey("owner")) {
            table.setOwner(owner);
            table.setOwnerSource("MANUAL");
        }
        tableMetadataRepository.save(table);

        if (requestBody.containsKey("tagNames")) {
            replaceTags(id, resolveTagNames(requestBody.get("tagNames")), currentUsername(request));
        }
        return getManagement(id);
    }

    @GetMapping("/table/{id}/permissions")
    @Operation(summary = "查询表权限", description = "查询指定表当前已授权的用户、权限来源和授权时间。")
    public List<UserResourceAccess> getTablePermissions(@PathVariable Long id) {
        TableMetadata table = getTableOrThrow(id);
        return userResourceAccessRepository.findActivePermissionsForTable(
                table.getClusterCode(), table.getDbName(), table.getTableName());
    }

    @GetMapping("/themes")
    public List<DataTheme> getThemes() {
        return dataThemeRepository.findByStatusOrderBySortOrderAscThemeNameAsc("ACTIVE");
    }

    @PostMapping("/themes")
    public DataTheme createTheme(@RequestBody DataTheme theme, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护数据主题");
        if (theme.getThemeName() == null || theme.getThemeName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "主题名称不能为空");
        }
        theme.setId(null);
        theme.setThemeName(theme.getThemeName().trim());
        return dataThemeRepository.save(theme);
    }

    @PutMapping("/themes/{themeId}")
    public DataTheme updateTheme(@PathVariable Long themeId,
                                 @RequestBody DataTheme requestBody,
                                 HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护数据主题");
        DataTheme theme = dataThemeRepository.findById(themeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "主题不存在: " + themeId));
        if (requestBody.getThemeName() != null) {
            theme.setThemeName(requestBody.getThemeName().trim());
        }
        theme.setParentId(requestBody.getParentId());
        theme.setDescription(requestBody.getDescription());
        if (requestBody.getSortOrder() != null) {
            theme.setSortOrder(requestBody.getSortOrder());
        }
        if (requestBody.getStatus() != null) {
            theme.setStatus(requestBody.getStatus());
        }
        return dataThemeRepository.save(theme);
    }

    @GetMapping("/tags")
    public List<MetadataTag> getTags() {
        return metadataTagRepository.findAll();
    }

    @GetMapping("/metrics")
    public List<MetricDefinition> getMetrics(@RequestParam(required = false) Long tableId) {
        if (tableId != null) {
            return metricDefinitionRepository.findByTableIdOrderByUpdateTimeDesc(tableId);
        }
        return metricDefinitionRepository.findAll();
    }

    @PostMapping("/metrics")
    public MetricDefinition createMetric(@RequestBody MetricDefinition metric, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护指标定义");
        validateMetric(metric);
        metric.setId(null);
        metric.setMetricName(metric.getMetricName().trim());
        metric.setMetricCode(metric.getMetricCode().trim());
        return metricDefinitionRepository.save(metric);
    }

    @PutMapping("/metrics/{id}")
    public MetricDefinition updateMetric(@PathVariable Long id,
                                         @RequestBody MetricDefinition requestBody,
                                         HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护指标定义");
        MetricDefinition metric = metricDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "指标不存在: " + id));
        if (requestBody.getMetricName() != null) {
            metric.setMetricName(requestBody.getMetricName().trim());
        }
        if (requestBody.getMetricCode() != null) {
            metric.setMetricCode(requestBody.getMetricCode().trim());
        }
        metric.setBusinessDefinition(requestBody.getBusinessDefinition());
        metric.setCalculationLogic(requestBody.getCalculationLogic());
        metric.setTableId(requestBody.getTableId());
        metric.setOwner(requestBody.getOwner());
        if (requestBody.getStatus() != null) {
            metric.setStatus(requestBody.getStatus());
        }
        validateMetric(metric);
        return metricDefinitionRepository.save(metric);
    }

    @GetMapping("/table/{id}/governance-tasks")
    public List<GovernanceTask> getGovernanceTasks(@PathVariable Long id) {
        return governanceTaskRepository.findByTableId(id);
    }

    @PostMapping("/sync")
    public String syncMetadata() {
        return "Metadata sync started";
    }

    @PostMapping("/collect/{dataSourceId}")
    @Operation(summary = "触发单数据源采集", description = "创建并异步执行指定数据源的元数据采集任务。")
    public MetadataCollectionTask collectDataSource(@PathVariable Long dataSourceId, HttpServletRequest request) {
        MetadataCollectionTask task = collectionService.createTask(dataSourceId, "MANUAL", currentUsername(request));
        asyncRunner.run(task.getId());
        return task;
    }

    @PostMapping("/collect/all")
    @Operation(summary = "触发全部采集", description = "为全部 ACTIVE Hive 数据源创建并异步触发采集任务。")
    public List<MetadataCollectionTask> collectAll(HttpServletRequest request) {
        List<DataSourceConfig> dataSources = dataSourceSyncService.syncHiveMetastoreDataSources();
        List<MetadataCollectionTask> tasks = collectionService.createTasksForAll(dataSources, "MANUAL", currentUsername(request));
        for (MetadataCollectionTask task : tasks) {
            asyncRunner.run(task.getId());
        }
        return tasks;
    }

    @GetMapping("/collect/tasks")
    @Operation(summary = "查询采集任务列表", description = "返回最近的元数据采集任务列表。")
    public List<MetadataCollectionTask> latestTasks() {
        return collectionService.latestTasks();
    }

    @PutMapping("/table/{id}/owner")
    @Operation(summary = "维护表负责人", description = "仅 admin 或超级用户可修改表级负责人。")
    public TableMetadata updateOwner(@PathVariable Long id,
                                     @RequestBody Map<String, String> requestBody,
                                     HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护资产负责人");
        TableMetadata table = tableMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        String owner = requestBody.get("owner");
        table.setOwner(owner == null || owner.trim().isEmpty() ? null : owner.trim());
        table.setOwnerSource("MANUAL");
        return tableMetadataRepository.save(table);
    }

    @PostMapping("/table/{id}/sync")
    public String syncTable(@PathVariable Long id) {
        TableMetadata table = tableMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        
        DataSourceConfig config = dataSourceRepository.findById(table.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("Data Source not found: " + table.getDataSourceId()));
        
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Collector not found for type: " + config.getType());
        }
        
        MetadataCollectionResult result = collector.collectTable(config, table.getDbName(), table.getTableName());
        return "Sync completed: " + result.summary();
    }

    private Map<String, Object> node(String key, String title, String type) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("key", key);
        node.put("title", title);
        node.put("type", type);
        node.put("children", new ArrayList<Map<String, Object>>());
        return node;
    }

    private Map<String, Object> child(Map<String, Object> parent, String key, String title, String type) {
        for (Map<String, Object> item : children(parent)) {
            if (key.equals(item.get("key"))) {
                return item;
            }
        }
        Map<String, Object> item = node(key, title, type);
        children(parent).add(item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> children(Map<String, Object> node) {
        return (List<Map<String, Object>>) node.get("children");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isManagedHiveDataSource(DataSourceConfig dataSource) {
        if (dataSource == null || dataSource.getEndpointId() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(dataSource.getDeleted())) {
            return false;
        }
        return "HIVE".equalsIgnoreCase(firstNonBlank(dataSource.getType()));
    }

    private boolean isUserHiveTable(TableMetadata table) {
        if (table == null || table.getDbName() == null) {
            return false;
        }
        return !SYSTEM_HIVE_DATABASES.contains(table.getDbName().trim().toLowerCase());
    }

    private TableMetadata getTableOrThrow(Long id) {
        return tableMetadataRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found: " + id));
    }

    private List<MetadataTag> tagsForTable(Long tableId) {
        List<TableTagMapping> mappings = tableTagMappingRepository.findByTableId(tableId);
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = mappings.stream().map(TableTagMapping::getTagId).collect(Collectors.toList());
        return metadataTagRepository.findAllById(tagIds);
    }

    private void replaceTags(Long tableId, Set<String> tagNames, String username) {
        tableTagMappingRepository.deleteByTableId(tableId);
        for (String tagName : tagNames) {
            MetadataTag tag = metadataTagRepository.findByTagName(tagName).orElseGet(() -> {
                MetadataTag item = new MetadataTag();
                item.setTagName(tagName);
                item.setTagType("CUSTOM");
                item.setColor("blue");
                return metadataTagRepository.save(item);
            });
            TableTagMapping mapping = new TableTagMapping();
            mapping.setTableId(tableId);
            mapping.setTagId(tag.getId());
            mapping.setAssignedBy(username);
            tableTagMappingRepository.save(mapping);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> resolveTagNames(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptySet();
        }
        Set<String> tagNames = new LinkedHashSet<>();
        if (rawValue instanceof Iterable) {
            for (Object item : (Iterable<Object>) rawValue) {
                String value = clean(asString(item));
                if (value != null && !value.isEmpty()) {
                    tagNames.add(value);
                }
            }
        } else {
            String value = asString(rawValue);
            if (value != null) {
                for (String item : value.split(",")) {
                    String tagName = clean(item);
                    if (tagName != null && !tagName.isEmpty()) {
                        tagNames.add(tagName);
                    }
                }
            }
        }
        return tagNames;
    }

    private void validateMetric(MetricDefinition metric) {
        if (metric.getMetricName() == null || metric.getMetricName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指标名称不能为空");
        }
        if (metric.getMetricCode() == null || metric.getMetricCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指标编码不能为空");
        }
        Optional<MetricDefinition> existing = metricDefinitionRepository.findByMetricCode(metric.getMetricCode().trim());
        if (existing.isPresent() && (metric.getId() == null || !existing.get().getId().equals(metric.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指标编码已存在: " + metric.getMetricCode());
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long asLong(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private ClusterEndpoint resolveHiveServer2Endpoint(TableMetadata table) {
        String clusterCode = table.getClusterCode();
        if ((clusterCode == null || clusterCode.trim().isEmpty()) && table.getDataSourceId() != null) {
            Optional<DataSourceConfig> dataSource = dataSourceRepository.findById(table.getDataSourceId());
            if (dataSource.isPresent()) {
                clusterCode = dataSource.get().getClusterCode();
            }
        }
        if (clusterCode == null || clusterCode.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前表缺少集群编码，无法定位 HIVE_SERVER2 端点");
        }
        List<ClusterEndpoint> endpoints = clusterEndpointRepository.findByClusterCodeAndEndpointTypeAndStatus(
                clusterCode, ClusterEndpoint.TYPE_HIVE_SERVER2, "ACTIVE");
        if (endpoints.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "集群未配置 ACTIVE HIVE_SERVER2 端点: " + clusterCode);
        }
        return endpoints.get(0);
    }

    private String quoteHiveIdentifier(String value) {
        return "`" + firstNonBlank(value).replace("`", "``") + "`";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String readableMessage(Exception e) {
        if (e == null) {
            return "未知错误";
        }
        if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            return e.getMessage();
        }
        if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().trim().isEmpty()) {
            return e.getCause().getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private String currentUsername(HttpServletRequest request) {
        return CurrentUser.usernameOrUnknown();
    }
}
