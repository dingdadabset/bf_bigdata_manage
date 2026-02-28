package com.dga.cluster.service;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClusterService {

    @Autowired
    private ClusterRepository clusterRepository;

    public List<Cluster> getAllClusters() {
        return clusterRepository.findActiveClusters();
    }

    public List<Cluster> getActiveClusters() {
        return clusterRepository.findByStatus("ACTIVE");
    }

    public Cluster getClusterById(Long id) {
        return clusterRepository.findById(id).orElse(null);
    }

    public Cluster createCluster(Cluster cluster) {
        // Ensure clusterName is unique
        Cluster existing = clusterRepository.findByClusterName(cluster.getClusterName());
        if (existing != null) {
            // Check if it's a deleted cluster, we can reactivate it
            if ("DELETED".equals(existing.getStatus())) {
                existing.setStatus("ACTIVE");
                existing.setType(cluster.getType());
                existing.setDescription(cluster.getDescription());
                return clusterRepository.save(existing);
            }
            throw new RuntimeException("Cluster name already exists");
        }
        return clusterRepository.save(cluster);
    }

    public Cluster updateCluster(Long id, Cluster cluster) {
        Optional<Cluster> existing = clusterRepository.findById(id);
        if (existing.isPresent()) {
            Cluster c = existing.get();
            c.setClusterName(cluster.getClusterName());
            c.setType(cluster.getType());
            c.setDescription(cluster.getDescription());
            c.setStatus(cluster.getStatus());
            return clusterRepository.save(c);
        }
        return null;
    }

    public void deleteCluster(Long id) {
        Optional<Cluster> existing = clusterRepository.findById(id);
        if (existing.isPresent()) {
            Cluster c = existing.get();
            c.setStatus("DELETED");
            clusterRepository.save(c);
        }
    }
}
