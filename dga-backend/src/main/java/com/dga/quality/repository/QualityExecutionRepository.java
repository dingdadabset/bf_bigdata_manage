package com.dga.quality.repository;

import com.dga.quality.entity.QualityExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityExecutionRepository extends JpaRepository<QualityExecution, Long> {
    List<QualityExecution> findByRuleIdOrderByExecutedAtDesc(Long ruleId);
}
