package com.dga.lineage.repository;

import com.dga.lineage.entity.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataLineageRepository extends JpaRepository<DataLineage, Long> {
    List<DataLineage> findBySourceTableId(Long sourceTableId);
    List<DataLineage> findByTargetTableId(Long targetTableId);
    
    // Find specific lineage edge to avoid duplicates
    Optional<DataLineage> findBySourceTableIdAndTargetTableId(Long sourceTableId, Long targetTableId);
}
