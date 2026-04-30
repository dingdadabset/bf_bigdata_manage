package com.dga.quality.repository;

import com.dga.quality.entity.QualityRule;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityRuleRepository extends JpaRepository<QualityRule, Long>, JpaSpecificationExecutor<QualityRule> {
    List<QualityRule> findByTableId(Long tableId);
    long countByStatus(String status);
    long countByLastExecutionStatus(String lastExecutionStatus);
}
