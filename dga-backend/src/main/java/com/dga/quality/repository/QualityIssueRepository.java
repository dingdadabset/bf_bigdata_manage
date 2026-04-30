package com.dga.quality.repository;

import com.dga.quality.entity.QualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityIssueRepository extends JpaRepository<QualityIssue, Long>, JpaSpecificationExecutor<QualityIssue> {
    long countByStatus(String status);
    Optional<QualityIssue> findFirstByRuleIdAndStatusOrderByLastSeenAtDesc(Long ruleId, String status);
}
