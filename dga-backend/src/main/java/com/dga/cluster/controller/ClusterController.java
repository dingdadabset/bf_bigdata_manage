package com.dga.cluster.controller;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{clusterCode}/endpoints")
    public List<ClusterEndpoint> getEndpoints(@PathVariable String clusterCode) {
        return clusterService.getEndpoints(clusterCode);
    }

    @PostMapping("/{clusterCode}/endpoints")
    public ClusterEndpoint createEndpoint(@PathVariable String clusterCode, @RequestBody ClusterEndpoint endpoint) {
        return clusterService.saveEndpoint(clusterCode, endpoint);
    }

    @PutMapping("/{clusterCode}/endpoints/{endpointId}")
    public ClusterEndpoint updateEndpoint(@PathVariable String clusterCode,
                                          @PathVariable Long endpointId,
                                          @RequestBody ClusterEndpoint endpoint) {
        endpoint.setId(endpointId);
        return clusterService.saveEndpoint(clusterCode, endpoint);
    }

    @PostMapping("/{clusterCode}/endpoints/test")
    public Map<String, Object> testEndpoint(@PathVariable String clusterCode, @RequestBody ClusterEndpoint endpoint) {
        return clusterService.testEndpoint(clusterCode, endpoint);
    }

    @DeleteMapping("/{clusterCode}/endpoints/{endpointId}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String clusterCode, @PathVariable Long endpointId) {
        clusterService.deleteEndpoint(endpointId);
        return ResponseEntity.ok().build();
    }
}
