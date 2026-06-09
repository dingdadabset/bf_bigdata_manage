package com.dga.access.repository;

import com.dga.access.entity.AuthRoleAssignmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRoleAssignmentAuditRepository extends JpaRepository<AuthRoleAssignmentAudit, Long> {
    List<AuthRoleAssignmentAudit> findTop50ByRoleCodeOrderByActionTimeDesc(String roleCode);

    @Query("SELECT a FROM AuthRoleAssignmentAudit a WHERE (:cluster IS NULL OR :cluster = '' OR a.cluster = :cluster) " +
            "AND (LOWER(a.subjectName) = LOWER(:username) OR LOWER(a.message) LIKE LOWER(CONCAT('%', :username, '%'))) " +
            "ORDER BY a.actionTime DESC")
    List<AuthRoleAssignmentAudit> findUserTimelineAudits(@Param("username") String username,
                                                         @Param("cluster") String cluster);
}
