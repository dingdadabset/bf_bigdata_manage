package com.dga.datasource.service;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataSourceSyncService {

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Transactional
    public List<DataSourceConfig> syncHiveMetastoreDataSources() {
        List<ClusterEndpoint> endpoints = endpointRepository.findByEndpointTypeAndStatus(
                ClusterEndpoint.TYPE_HIVE_METASTORE_DB, "ACTIVE");
        Set<Long> activeEndpointIds = new HashSet<>();

        for (ClusterEndpoint endpoint : endpoints) {
            if (endpoint.getId() == null) {
                continue;
            }
            activeEndpointIds.add(endpoint.getId());
            Cluster cluster = clusterRepository.findByClusterCode(endpoint.getClusterCode());
            String clusterName = cluster == null ? endpoint.getClusterCode() : cluster.getClusterName();

            DataSourceConfig config = dataSourceRepository
                    .findByClusterCodeAndEndpointIdAndType(endpoint.getClusterCode(), endpoint.getId(), "HIVE")
                    .orElseGet(DataSourceConfig::new);
            config.setName(clusterName + " Hive Metastore");
            config.setType("HIVE");
            config.setClusterCode(endpoint.getClusterCode());
            config.setClusterName(clusterName);
            config.setEndpointId(endpoint.getId());
            config.setUrl(endpoint.getUrl());
            config.setUsername(endpoint.getUsername());
            config.setPassword(endpoint.getPassword());
            config.setDescription(endpoint.getDescription());
            config.setStatus("ACTIVE");
            config.setDeleted(false);
            dataSourceRepository.save(config);
        }

        for (DataSourceConfig config : dataSourceRepository.findByTypeAndEndpointIdIsNotNull("HIVE")) {
            Long endpointId = config.getEndpointId();
            if (endpointId != null && !activeEndpointIds.contains(endpointId)) {
                config.setDeleted(true);
                config.setStatus("INACTIVE");
                dataSourceRepository.save(config);
            }
        }

        return dataSourceRepository.findActiveDataSources();
    }
}
