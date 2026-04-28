package com.dga.cluster.repository;

import com.dga.cluster.entity.ClusterEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterEndpointRepository extends JpaRepository<ClusterEndpoint, Long> {
    List<ClusterEndpoint> findByClusterCodeAndStatusNot(String clusterCode, String status);
    List<ClusterEndpoint> findByClusterCodeAndEndpointTypeAndStatus(String clusterCode, String endpointType, String status);
    List<ClusterEndpoint> findByClusterCodeAndAuthBackendAndStatus(String clusterCode, String authBackend, String status);
    List<ClusterEndpoint> findByEndpointTypeAndStatus(String endpointType, String status);
}
