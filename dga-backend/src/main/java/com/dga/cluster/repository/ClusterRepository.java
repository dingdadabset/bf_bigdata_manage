package com.dga.cluster.repository;

import com.dga.cluster.entity.Cluster;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    Cluster findByClusterName(String clusterName);
    Cluster findByClusterCode(String clusterCode);
    List<Cluster> findByStatus(String status);
    
    @Query("SELECT c FROM Cluster c WHERE c.status IS NULL OR c.status <> 'DELETED'")
    List<Cluster> findActiveClusters();
}
