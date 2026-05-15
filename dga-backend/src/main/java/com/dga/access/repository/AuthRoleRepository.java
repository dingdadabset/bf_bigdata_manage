package com.dga.access.repository;

import com.dga.access.entity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRoleRepository extends JpaRepository<AuthRole, Long> {
    AuthRole findByRoleCode(String roleCode);
    boolean existsByRoleCode(String roleCode);
    List<AuthRole> findByClusterAndStatusNotOrderByCreateTimeDesc(String cluster, String status);
    List<AuthRole> findByStatusNotOrderByCreateTimeDesc(String status);
}
