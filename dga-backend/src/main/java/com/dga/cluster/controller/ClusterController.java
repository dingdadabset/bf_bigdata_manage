package com.dga.cluster.controller;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clusters")
@CrossOrigin
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @GetMapping
    public List<Cluster> getAllClusters() {
        return clusterService.getAllClusters();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cluster> getClusterById(@PathVariable Long id) {
        Cluster cluster = clusterService.getClusterById(id);
        if (cluster != null) {
            return ResponseEntity.ok(cluster);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Cluster createCluster(@RequestBody Cluster cluster) {
        return clusterService.createCluster(cluster);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cluster> updateCluster(@PathVariable Long id, @RequestBody Cluster cluster) {
        Cluster updated = clusterService.updateCluster(id, cluster);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(@PathVariable Long id) {
        clusterService.deleteCluster(id);
        return ResponseEntity.ok().build();
    }
}
