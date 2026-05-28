package com.dga.access.repository;

import com.dga.access.entity.AccessGovernanceAiPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessGovernanceAiPlanRepository extends JpaRepository<AccessGovernanceAiPlan, Long> {
}
