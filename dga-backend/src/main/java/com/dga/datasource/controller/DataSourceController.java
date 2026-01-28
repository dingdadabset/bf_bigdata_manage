package com.dga.datasource.controller;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.metadata.service.MetadataCollector;
import com.dga.metadata.service.MetadataCollectorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasource")
@CrossOrigin
public class DataSourceController {

    @Autowired
    private DataSourceConfigRepository repository;

    @Autowired
    private MetadataCollectorFactory collectorFactory;

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
        collector.collectMetadata(config);
        return "Collection triggered for " + config.getName();
    }
}
