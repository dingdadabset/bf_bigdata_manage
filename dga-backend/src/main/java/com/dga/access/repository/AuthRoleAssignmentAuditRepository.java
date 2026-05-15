package com.dga.access.repository;

import com.dga.access.entity.AuthRoleAssignmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRoleAssignmentAuditRepository extends JpaRepository<AuthRoleAssignmentAudit, Long> {
    List<AuthRoleAssignmentAudit> findTop50ByRoleCodeOrderByActionTimeDesc(String roleCode);
}
