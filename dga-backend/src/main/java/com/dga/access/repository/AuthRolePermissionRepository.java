package com.dga.access.repository;

import com.dga.access.entity.AuthRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRolePermissionRepository extends JpaRepository<AuthRolePermission, Long> {
    List<AuthRolePermission> findByRoleCodeAndStatus(String roleCode, String status);
    List<AuthRolePermission> findByRoleCode(String roleCode);
}
