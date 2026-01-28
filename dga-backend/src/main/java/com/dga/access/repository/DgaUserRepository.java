package com.dga.access.repository;

import com.dga.access.entity.DgaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface DgaUserRepository extends JpaRepository<DgaUser, Long> {
    boolean existsByUsername(String username);
    DgaUser findByUsername(String username);
    
    org.springframework.data.domain.Page<DgaUser> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);
    
    org.springframework.data.domain.Page<DgaUser> findByClusterNameAndIsDeletedFalse(String clusterName, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<DgaUser> findByIsDeletedFalseAndCreationStrategyNotIn(List<String> strategies, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<DgaUser> findByClusterNameAndIsDeletedFalseAndCreationStrategyNotIn(String clusterName, List<String> strategies, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT u.clusterName FROM DgaUser u WHERE u.isDeleted = false AND u.clusterName IS NOT NULL")
    List<String> findDistinctClusterNames();
}
