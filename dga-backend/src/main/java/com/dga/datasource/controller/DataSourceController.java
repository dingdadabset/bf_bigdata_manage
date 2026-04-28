package com.dga.datasource.controller;

import com.dga.access.service.AdminGuard;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.datasource.service.DataSourceSyncService;
import com.dga.metadata.entity.MetadataCollectionTask;
import com.dga.metadata.service.MetadataCollector;
import com.dga.metadata.service.MetadataCollectorFactory;
import com.dga.metadata.service.MetadataCollectionAsyncRunner;
import com.dga.metadata.service.MetadataCollectionService;
import com.dga.metadata.service.SyncStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource")
@CrossOrigin
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
    public List<DataSourceConfig> list() {
        return dataSourceSyncService.syncHiveMetastoreDataSources();
    }

    @PostMapping
    public DataSourceConfig add(@RequestBody DataSourceConfig config) {
        return repository.save(config);
    }

    @PostMapping("/test-connection")
    public boolean testConnection(@RequestBody DataSourceConfig config) {
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Unsupported Data Source Type: " + config.getType());
        }
        return collector.testConnection(config);
    }

    @PostMapping("/test/{id}")
    public boolean testStoredConnection(@PathVariable Long id) {
        DataSourceConfig config = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Unsupported Data Source Type: " + config.getType());
        }
        return collector.testConnection(config);
    }

    @PostMapping("/collect/{id}")
    public MetadataCollectionTask triggerCollection(@PathVariable Long id, HttpServletRequest request) {
        String key = "datasource-" + id;
        syncStatusService.setStatus(key, "RUNNING", "Collecting metadata");
        MetadataCollectionTask task = collectionService.createTask(id, "MANUAL", currentUsername(request));
        asyncRunner.run(task.getId());
        return task;
    }

    @GetMapping("/collect/status/{id}")
    public Map<String, Object> getCollectionStatus(@PathVariable Long id) {
        String key = "datasource-" + id;
        Map<String, Object> result = syncStatusService.getFullStatus(key);
        result.put("latestTask", collectionService.latestTask(id));
        return result;
    }

    @GetMapping("/collect/tasks")
    public List<MetadataCollectionTask> latestTasks() {
        return collectionService.latestTasks();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        adminGuard.requireDeletePrivilege(request);
        repository.deleteById(id);
    }

    private String currentUsername(HttpServletRequest request) {
        String username = request == null ? null : request.getHeader("X-DGA-Username");
        return username == null || username.trim().isEmpty() ? "unknown" : username.trim();
    }
}
