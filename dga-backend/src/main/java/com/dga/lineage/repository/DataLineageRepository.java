package com.dga.lineage.repository;

import com.dga.lineage.entity.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataLineageRepository extends JpaRepository<DataLineage, Long> {
    List<DataLineage> findBySourceTableId(Long sourceTableId);
    List<DataLineage> findByTargetTableId(Long targetTableId);
    List<DataLineage> findBySourceTableIdAndStatus(Long sourceTableId, String status);
    List<DataLineage> findByTargetTableIdAndStatus(Long targetTableId, String status);
    List<DataLineage> findBySourceTableIdAndSourceTypeAndStatus(Long sourceTableId, String sourceType, String status);
    List<DataLineage> findByTargetTableIdAndSourceTypeAndStatus(Long targetTableId, String sourceType, String status);
    List<DataLineage> findBySourceTableIdAndSourceEndpointIdAndStatus(Long sourceTableId, Long sourceEndpointId, String status);
    List<DataLineage> findByTargetTableIdAndSourceEndpointIdAndStatus(Long targetTableId, Long sourceEndpointId, String status);

    @Query("SELECT d FROM DataLineage d WHERE d.targetTableId = :tableId " +
            "AND (d.status = 'ACTIVE' OR d.status IS NULL) " +
            "AND (:sourceType IS NULL OR d.sourceType = :sourceType) " +
            "AND (:sourceEndpointId IS NULL OR d.sourceEndpointId = :sourceEndpointId)")
    List<DataLineage> findActiveUpstream(@Param("tableId") Long tableId,
                                         @Param("sourceType") String sourceType,
                                         @Param("sourceEndpointId") Long sourceEndpointId);

    @Query("SELECT d FROM DataLineage d WHERE d.sourceTableId = :tableId " +
            "AND (d.status = 'ACTIVE' OR d.status IS NULL) " +
            "AND (:sourceType IS NULL OR d.sourceType = :sourceType) " +
            "AND (:sourceEndpointId IS NULL OR d.sourceEndpointId = :sourceEndpointId)")
    List<DataLineage> findActiveDownstream(@Param("tableId") Long tableId,
                                           @Param("sourceType") String sourceType,
                                           @Param("sourceEndpointId") Long sourceEndpointId);
    
    // Find specific lineage edge to avoid duplicates
    Optional<DataLineage> findBySourceTableIdAndTargetTableId(Long sourceTableId, Long targetTableId);

    @Modifying
    @Transactional
    @Query("UPDATE DataLineage d SET d.status = 'EXPIRED' WHERE d.sourceEndpointId = ?1 AND d.dataSourceId = ?2 AND d.status = 'ACTIVE'")
    int expireActiveBySourceEndpointAndDataSource(Long sourceEndpointId, Long dataSourceId);

    @Modifying
    @Transactional
    @Query("UPDATE DataLineage d SET d.status = 'EXPIRED' WHERE d.sourceEndpointId = ?1 AND d.dataSourceId = ?2 AND d.status = 'ACTIVE' AND (d.runId IS NULL OR d.runId <> ?3)")
    int expireActiveBySourceEndpointAndDataSourceExceptRun(Long sourceEndpointId, Long dataSourceId, String runId);

    @Modifying
    @Transactional
    @Query("UPDATE DataLineage d SET d.status = ?2 WHERE d.sourceEndpointId = ?1 AND d.status = 'ACTIVE'")
    int updateActiveStatusBySourceEndpointId(Long sourceEndpointId, String status);
}
