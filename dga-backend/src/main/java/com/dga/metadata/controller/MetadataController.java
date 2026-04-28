package com.dga.metadata.controller;

import com.dga.access.service.AdminGuard;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.UserResourceAccessRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

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

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long tableCount = tableMetadataRepository.count();
        Long totalSize = tableMetadataRepository.sumTotalSize();
        Double avgScore = tableMetadataRepository.avgGovernanceScore();
        long todaySyncCount = tableMetadataRepository.countBySyncTimeAfter(LocalDateTime.now().with(LocalTime.MIN));

        stats.put("tableCount", tableCount);
        stats.put("totalSize", totalSize != null ? totalSize : 0L);
        stats.put("avgScore", avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0);
        stats.put("todaySyncCount", todaySyncCount);
        
        return stats;
    }

    @GetMapping("/tables")
    public List<TableMetadata> getTables(@RequestParam(required = false) Long dataSourceId) {
        if (dataSourceId != null) {
            return tableMetadataRepository.findByDataSourceId(dataSourceId);
        }
        return tableMetadataRepository.findAll();
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
                tables.stream().map(TableMetadata::getDataSourceId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(DataSourceConfig::getId, item -> item));

        Map<String, Map<String, Object>> clusters = new LinkedHashMap<>();
        for (TableMetadata table : tables) {
            DataSourceConfig ds = dataSourceMap.get(table.getDataSourceId());
            String resolvedClusterCode = firstNonBlank(table.getClusterCode(), ds == null ? null : ds.getClusterCode(), "UNKNOWN");
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
    public TableMetadata getTable(@PathVariable Long id) {
        return tableMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
    }

    @GetMapping("/table/{id}/columns")
    public List<ColumnMetadata> getColumns(@PathVariable Long id) {
        return columnMetadataRepository.findByTableId(id);
    }

    @GetMapping("/table/{id}/partitions")
    public Map<String, Object> getPartitions(@PathVariable Long id) {
        TableMetadata table = getTableOrThrow(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tableId", id);
        result.put("partitionCount", table.getPartitionCount() == null ? 0L : table.getPartitionCount());
        result.put("items", partitionMetadataRepository.findByTableIdOrderByLastModifyTimeDescIdDesc(id));
        return result;
    }

    @GetMapping("/table/{id}/lineage")
    public Map<String, Object> getLineage(@PathVariable Long id,
                                          @RequestParam(required = false) String sourceType,
                                          @RequestParam(required = false) Long sourceEndpointId) {
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
    public MetadataCollectionTask collectDataSource(@PathVariable Long dataSourceId, HttpServletRequest request) {
        MetadataCollectionTask task = collectionService.createTask(dataSourceId, "MANUAL", currentUsername(request));
        asyncRunner.run(task.getId());
        return task;
    }

    @PostMapping("/collect/all")
    public List<MetadataCollectionTask> collectAll(HttpServletRequest request) {
        List<DataSourceConfig> dataSources = dataSourceSyncService.syncHiveMetastoreDataSources();
        List<MetadataCollectionTask> tasks = collectionService.createTasksForAll(dataSources, "MANUAL", currentUsername(request));
        for (MetadataCollectionTask task : tasks) {
            asyncRunner.run(task.getId());
        }
        return tasks;
    }

    @GetMapping("/collect/tasks")
    public List<MetadataCollectionTask> latestTasks() {
        return collectionService.latestTasks();
    }

    @PutMapping("/table/{id}/owner")
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

    private String currentUsername(HttpServletRequest request) {
        String username = request == null ? null : request.getHeader("X-DGA-Username");
        return username == null || username.trim().isEmpty() ? "unknown" : username.trim();
    }
}
