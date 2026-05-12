package com.dga.metadata.repository;

import com.dga.metadata.entity.MetadataContextSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MetadataContextSuggestionRepository extends JpaRepository<MetadataContextSuggestion, Long> {
    List<MetadataContextSuggestion> findByTableIdOrderByStatusAscParsedAtDescIdDesc(Long tableId);

    @Modifying
    @Transactional
    @Query("UPDATE MetadataContextSuggestion s SET s.status = 'REJECTED' " +
            "WHERE s.sourceEndpointId = :sourceEndpointId AND s.dataSourceId = :dataSourceId " +
            "AND s.status = 'PENDING' AND (s.runId IS NULL OR s.runId <> :runId)")
    int rejectPendingBySourceEndpointAndDataSourceExceptRun(@Param("sourceEndpointId") Long sourceEndpointId,
                                                            @Param("dataSourceId") Long dataSourceId,
                                                            @Param("runId") String runId);
}
