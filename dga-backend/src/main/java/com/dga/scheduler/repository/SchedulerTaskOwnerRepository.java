package com.dga.scheduler.repository;

import com.dga.scheduler.entity.SchedulerTaskOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulerTaskOwnerRepository extends JpaRepository<SchedulerTaskOwner, Long> {
    Optional<SchedulerTaskOwner> findByClusterCodeAndSourceEndpointIdAndProjectNameAndFlowNameAndTaskName(
            String clusterCode, Long sourceEndpointId, String projectName, String flowName, String taskName);

    List<SchedulerTaskOwner> findByClusterCodeAndSourceEndpointIdAndStatus(
            String clusterCode, Long sourceEndpointId, String status);
}
