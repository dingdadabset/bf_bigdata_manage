package com.dga.datasource.repository;

import com.dga.datasource.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {
    Optional<DataSourceConfig> findByClusterCodeAndEndpointIdAndType(String clusterCode, Long endpointId, String type);
    @Query(value = "SELECT * FROM data_source_config WHERE is_deleted = false OR is_deleted IS NULL ORDER BY name ASC", nativeQuery = true)
    List<DataSourceConfig> findActiveDataSources();
    @Query(value = "SELECT * FROM data_source_config WHERE type = 'HIVE' AND endpoint_id IS NOT NULL AND (is_deleted = false OR is_deleted IS NULL) ORDER BY name ASC", nativeQuery = true)
    List<DataSourceConfig> findActiveHiveMetastoreDataSources();
    @Query(value = "SELECT * FROM data_source_config WHERE type = ?1 AND endpoint_id IS NOT NULL", nativeQuery = true)
    List<DataSourceConfig> findByTypeAndEndpointIdIsNotNull(String type);
}
