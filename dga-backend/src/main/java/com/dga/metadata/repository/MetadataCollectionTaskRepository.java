package com.dga.metadata.repository;

import com.dga.metadata.entity.MetadataCollectionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataCollectionTaskRepository extends JpaRepository<MetadataCollectionTask, Long> {
    boolean existsByDataSourceIdAndStatus(Long dataSourceId, String status);
    Optional<MetadataCollectionTask> findTopByDataSourceIdOrderByStartedAtDesc(Long dataSourceId);
    List<MetadataCollectionTask> findTop20ByOrderByStartedAtDesc();
}
