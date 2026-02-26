package com.dga.datasource.controller;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.metadata.service.MetadataCollector;
import com.dga.metadata.service.MetadataCollectorFactory;
import com.dga.metadata.service.SyncStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public List<DataSourceConfig> list() {
        return repository.findAll();
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
    
    @PostMapping("/collect/{id}")
    public String triggerCollection(@PathVariable Long id) {
        DataSourceConfig config = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        MetadataCollector collector = collectorFactory.getCollector(config.getType());
        if (collector == null) {
            throw new RuntimeException("Unsupported Data Source Type: " + config.getType());
        }
        String key = "datasource-" + id;
        syncStatusService.setStatus(key, "RUNNING", "Collecting metadata for " + config.getName());
        collectMetadataAsync(collector, config, key);
        return "Collection triggered for " + config.getName();
    }

    @Async("metadataTaskExecutor")
    protected void collectMetadataAsync(MetadataCollector collector, DataSourceConfig config, String key) {
        try {
            collector.collectMetadata(config);
            syncStatusService.setStatus(key, "SUCCESS", "Metadata collection completed");
        } catch (Exception e) {
            syncStatusService.setStatus(key, "FAILED", "Metadata collection failed");
        }
    }

    @GetMapping("/collect/status/{id}")
    public Map<String, Object> getCollectionStatus(@PathVariable Long id) {
        String key = "datasource-" + id;
        return syncStatusService.getFullStatus(key);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
