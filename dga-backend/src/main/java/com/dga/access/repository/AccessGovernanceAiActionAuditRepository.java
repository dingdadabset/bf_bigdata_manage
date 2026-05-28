package com.dga.access.repository;

import com.dga.access.entity.AccessGovernanceAiActionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessGovernanceAiActionAuditRepository extends JpaRepository<AccessGovernanceAiActionAudit, Long> {
}
