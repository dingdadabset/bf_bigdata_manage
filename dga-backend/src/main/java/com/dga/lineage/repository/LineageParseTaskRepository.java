package com.dga.lineage.repository;

import com.dga.lineage.entity.LineageParseTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineageParseTaskRepository extends JpaRepository<LineageParseTask, Long> {
    List<LineageParseTask> findTop50ByOrderByStartedAtDesc();
    List<LineageParseTask> findTop50BySourceEndpointIdOrderByStartedAtDesc(Long sourceEndpointId);
    List<LineageParseTask> findTop50ByDataSourceIdOrderByStartedAtDesc(Long dataSourceId);
    List<LineageParseTask> findTop50BySourceEndpointIdAndDataSourceIdOrderByStartedAtDesc(Long sourceEndpointId, Long dataSourceId);
}
