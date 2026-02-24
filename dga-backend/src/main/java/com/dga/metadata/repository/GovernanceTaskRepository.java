package com.dga.metadata.repository;

import com.dga.metadata.entity.GovernanceTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernanceTaskRepository extends JpaRepository<GovernanceTask, Long> {
    List<GovernanceTask> findByTableId(Long tableId);
}
