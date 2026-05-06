package com.dga.datasource.controller;

import com.dga.access.service.AdminGuard;
import com.dga.access.security.CurrentUser;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.datasource.service.DataSourceSyncService;
import com.dga.metadata.entity.MetadataCollectionTask;
import com.dga.metadata.service.MetadataCollector;
import com.dga.metadata.service.MetadataCollectorFactory;
import com.dga.metadata.service.MetadataCollectionAsyncRunner;
import com.dga.metadata.service.MetadataCollectionService;
import com.dga.metadata.service.SyncStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource")
@CrossOrigin
@Tag(name = "数据源管理", description = "Hive 元数据采集数据源管理、连通性测试与采集任务触发")
@SecurityRequirement(name = "bearerAuth")
public class DataSourceController {

    @Autowired
    private DataSourceConfigRepository repository;

    @Autowired
    private MetadataCollectorFactory collectorFactory;

    @Autowired
    private SyncStatusService syncStatusService;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private DataSourceSyncService dataSourceSyncService;

    @Autowired
    private MetadataCollectionService collectionService;

    @Autowired
    private MetadataCollectionAsyncRunner asyncRunner;

    @GetMapping
    @Operation(summary = "查询数据源列表", description = "返回当前环境资源同步后的 Hive Metastore 数据源列表。")
    public List<DataSourceConfig> list() {
        return dataSourceSyncService.syncHiveMetastoreDataSources();
    }

    @PostMapping
    public DataSourceConfig add(@RequestBody DataSourceConfig config) {
        return repository.save(config);
    }

    @PostMapping("/test-connection")
    @Operation(summary = "测试数据源连接", description = "基于提交的数据源配置即时测试连接是否可用。")
    public boolean testConnection(@RequestBody DataSourceConfig config) {
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Unsupported Data Source Type: " + config.getType());
        }
        return collector.testConnection(config);
    }

    @PostMapping("/test/{id}")
    @Operation(summary = "测试已保存数据源连接", description = "按数据源 ID 测试已保存的数据源连接。")
    public boolean testStoredConnection(@PathVariable Long id) {
        DataSourceConfig config = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Unsupported Data Source Type: " + config.getType());
        }
        return collector.testConnection(config);
    }

    @PostMapping("/collect/{id}")
    @Operation(summary = "触发单数据源采集", description = "创建并异步执行指定数据源的元数据采集任务。")
    public MetadataCollectionTask triggerCollection(@PathVariable Long id, HttpServletRequest request) {
        String key = "datasource-" + id;
        syncStatusService.setStatus(key, "RUNNING", "Collecting metadata");
        MetadataCollectionTask task = collectionService.createTask(id, "MANUAL", currentUsername(request));
        asyncRunner.run(task.getId());
        return task;
    }

    @GetMapping("/collect/status/{id}")
    @Operation(summary = "查询采集状态", description = "查询指定数据源的最新采集状态和最近任务信息。")
    public Map<String, Object> getCollectionStatus(@PathVariable Long id) {
        String key = "datasource-" + id;
        Map<String, Object> result = syncStatusService.getFullStatus(key);
        result.put("latestTask", collectionService.latestTask(id));
        return result;
    }

    @GetMapping("/collect/tasks")
    @Operation(summary = "查询最近采集任务", description = "返回最近的元数据采集任务列表。")
    public List<MetadataCollectionTask> latestTasks() {
        return collectionService.latestTasks();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        adminGuard.requireDeletePrivilege(request);
        repository.deleteById(id);
    }

    private String currentUsername(HttpServletRequest request) {
        return CurrentUser.usernameOrUnknown();
    }
}
