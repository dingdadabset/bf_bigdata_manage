package com.dga.access.repository;

import com.dga.access.entity.AccessGovernanceAiAdvice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessGovernanceAiAdviceRepository extends JpaRepository<AccessGovernanceAiAdvice, Long> {
    List<AccessGovernanceAiAdvice> findTop5ByIssueIdOrderByCreatedAtDesc(Long issueId);
}
