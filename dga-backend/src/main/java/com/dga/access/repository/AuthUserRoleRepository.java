package com.dga.access.repository;

import com.dga.access.entity.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, Long> {
    List<AuthUserRole> findByRoleCodeAndStatus(String roleCode, String status);
    List<AuthUserRole> findByRoleCodeAndSubjectTypeAndStatus(String roleCode, String subjectType, String status);
    List<AuthUserRole> findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(String roleCode, String subjectType, String subjectName, String status);
    List<AuthUserRole> findBySubjectTypeAndSubjectNameAndStatus(String subjectType, String subjectName, String status);
}
