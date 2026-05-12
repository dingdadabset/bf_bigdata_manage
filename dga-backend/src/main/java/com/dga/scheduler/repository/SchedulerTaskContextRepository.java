package com.dga.scheduler.repository;

import com.dga.scheduler.entity.SchedulerTaskContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SchedulerTaskContextRepository extends JpaRepository<SchedulerTaskContext, Long> {

    @Query("SELECT c FROM SchedulerTaskContext c WHERE (c.status = 'ACTIVE' OR c.status IS NULL) " +
            "AND c.clusterCode = :clusterCode AND c.sourceEndpointId = :sourceEndpointId " +
            "AND c.projectName = :projectName " +
            "AND (:flowName IS NULL OR c.flowName = :flowName OR c.flowName IS NULL) " +
            "AND (:taskName IS NULL OR c.taskName = :taskName OR c.taskName IS NULL)")
    List<SchedulerTaskContext> findActiveBySchedulerContext(@Param("clusterCode") String clusterCode,
                                                            @Param("sourceEndpointId") Long sourceEndpointId,
                                                            @Param("projectName") String projectName,
                                                            @Param("flowName") String flowName,
                                                            @Param("taskName") String taskName);

    @Modifying
    @Transactional
    @Query("UPDATE SchedulerTaskContext c SET c.status = 'EXPIRED' " +
            "WHERE c.sourceEndpointId = :sourceEndpointId AND c.dataSourceId = :dataSourceId " +
            "AND c.status = 'ACTIVE' AND (c.runId IS NULL OR c.runId <> :runId)")
    int expireActiveBySourceEndpointAndDataSourceExceptRun(@Param("sourceEndpointId") Long sourceEndpointId,
                                                           @Param("dataSourceId") Long dataSourceId,
                                                           @Param("runId") String runId);
}
