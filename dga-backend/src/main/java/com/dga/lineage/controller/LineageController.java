package com.dga.lineage.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.dga.lineage.entity.LineageParseTask;
import com.dga.lineage.service.LineageCollectionService;
import com.dga.lineage.service.AzkabanLineageService;
import com.dga.lineage.service.LineageGraphService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lineage")
public class LineageController {

    @Autowired
    private AzkabanLineageService azkabanLineageService;

    @Autowired
    private LineageGraphService lineageGraphService;

    @Autowired
    private LineageCollectionService lineageCollectionService;

    @PostMapping("/collect/{endpointId}")
    public LineageParseTask collect(@PathVariable Long endpointId,
                                    @RequestParam Long dataSourceId,
                                    HttpServletRequest request) {
        return lineageCollectionService.collect(endpointId, dataSourceId, currentUsername(request));
    }

    @GetMapping("/tasks")
    public List<LineageParseTask> tasks(@RequestParam(required = false) Long sourceEndpointId,
                                        @RequestParam(required = false) Long dataSourceId) {
        return lineageCollectionService.latestTasks(sourceEndpointId, dataSourceId);
    }

    @PostMapping("/parse-azkaban")
    public String parseAzkaban(@RequestBody AzkabanConnectionRequest request) {
        try {
            azkabanLineageService.parseAzkabanLineage(
                    request.getHost(),
                    request.getPort(),
                    request.getDatabase(),
                    request.getUsername(),
                    request.getPassword(),
                    request.getClusterCode(),
                    request.getDataSourceId()
            );
            return "Azkaban lineage parsing started successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/table/{tableId}")
    public Map<String, Object> getTableLineage(@PathVariable Long tableId,
                                               @RequestParam(required = false) String sourceType,
                                               @RequestParam(required = false) Long sourceEndpointId) {
        return lineageGraphService.getLineageGraph(tableId, sourceType, sourceEndpointId);
    }

    private String currentUsername(HttpServletRequest request) {
        String username = request == null ? null : request.getHeader("X-DGA-Username");
        return username == null || username.trim().isEmpty() ? "unknown" : username.trim();
    }

    public static class AzkabanConnectionRequest {
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private String clusterCode;
        private Long dataSourceId;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getClusterCode() { return clusterCode; }
        public void setClusterCode(String clusterCode) { this.clusterCode = clusterCode; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    }
}
