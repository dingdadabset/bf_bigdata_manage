package com.dga.datamap.controller;

import com.dga.datamap.entity.UserRecentView;
import com.dga.datamap.repository.UserRecentViewRepository;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.metadata.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datamap")
@CrossOrigin
public class DataMapController {

    @Autowired
    private UserRecentViewRepository recentViewRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long instanceCount = dataSourceRepository.count();
        long tableCount = tableMetadataRepository.count();
        long dbCount = tableMetadataRepository.countDistinctDbName();

        stats.put("instanceCount", instanceCount);
        stats.put("databaseCount", dbCount);
        stats.put("tableCount", tableCount);
        stats.put("apiCount", 0); // Mock
        stats.put("collectorCount", 0); // Mock
        stats.put("managedCount", tableCount); // Assuming all collected are managed
        stats.put("coverage", instanceCount > 0 ? "100%" : "0%");

        return stats;
    }

    @GetMapping("/recent")
    public List<UserRecentView> getRecentViews(@RequestParam String username) {
        return recentViewRepository.findTopByUsername(username, PageRequest.of(0, 10));
    }

    @PostMapping("/recent")
    public UserRecentView addRecentView(@RequestBody UserRecentView view) {
        return recentViewRepository.save(view);
    }
}
