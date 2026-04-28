package com.dga.metadata.repository;

import com.dga.metadata.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, Long> {
    List<MetricDefinition> findByTableIdOrderByUpdateTimeDesc(Long tableId);

    Optional<MetricDefinition> findByMetricCode(String metricCode);
}
