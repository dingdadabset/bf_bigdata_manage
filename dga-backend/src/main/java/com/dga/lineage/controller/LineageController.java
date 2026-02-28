package com.dga.lineage.controller;

import java.util.List;
import java.util.Map;
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

    @PostMapping("/parse-azkaban")
    public String parseAzkaban(@RequestBody AzkabanConnectionRequest request) {
        try {
            azkabanLineageService.parseAzkabanLineage(
                    request.getHost(),
                    request.getPort(),
                    request.getDatabase(),
                    request.getUsername(),
                    request.getPassword()
            );
            return "Azkaban lineage parsing started successfully.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/table/{tableId}")
    public Map<String, Object> getTableLineage(@PathVariable Long tableId) {
        return lineageGraphService.getLineageGraph(tableId);
    }

    public static class AzkabanConnectionRequest {
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;

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
    }
}
