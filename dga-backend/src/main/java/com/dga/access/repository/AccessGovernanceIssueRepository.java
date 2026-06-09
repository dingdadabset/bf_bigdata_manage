package com.dga.access.repository;

import com.dga.access.entity.AccessGovernanceIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AccessGovernanceIssueRepository extends JpaRepository<AccessGovernanceIssue, Long> {

    @Query("SELECT i FROM AccessGovernanceIssue i WHERE i.issueKey = :issueKey AND i.status <> 'RESOLVED'")
    List<AccessGovernanceIssue> findOpenByIssueKey(@Param("issueKey") String issueKey);

    @Query("SELECT i FROM AccessGovernanceIssue i WHERE i.status <> 'RESOLVED' " +
            "AND (:cluster IS NULL OR :cluster = '' OR i.clusterCode = :cluster OR i.clusterName = :cluster)")
    List<AccessGovernanceIssue> findUnresolvedByCluster(@Param("cluster") String cluster);

    @Query(value = "SELECT i FROM AccessGovernanceIssue i WHERE " +
            "(:cluster IS NULL OR :cluster = '' OR i.clusterCode = :cluster OR i.clusterName = :cluster) " +
            "AND (:hasTypes = false OR i.issueType IN :types) " +
            "AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR i.status = :status) " +
            "AND (:username IS NULL OR :username = '' OR LOWER(i.username) LIKE LOWER(CONCAT('%', :username, '%')))",
            countQuery = "SELECT COUNT(i) FROM AccessGovernanceIssue i WHERE " +
                    "(:cluster IS NULL OR :cluster = '' OR i.clusterCode = :cluster OR i.clusterName = :cluster) " +
                    "AND (:hasTypes = false OR i.issueType IN :types) " +
                    "AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR i.status = :status) " +
                    "AND (:username IS NULL OR :username = '' OR LOWER(i.username) LIKE LOWER(CONCAT('%', :username, '%')))")
    Page<AccessGovernanceIssue> searchIssues(@Param("cluster") String cluster,
                                             @Param("hasTypes") boolean hasTypes,
                                             @Param("types") List<String> types,
                                             @Param("status") String status,
                                             @Param("username") String username,
                                             Pageable pageable);

    @Query("SELECT COUNT(i) FROM AccessGovernanceIssue i " +
            "WHERE (:cluster IS NULL OR :cluster = '' OR i.clusterCode = :cluster OR i.clusterName = :cluster)")
    long countByCluster(@Param("cluster") String cluster);

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessGovernanceIssue i " +
            "WHERE (:cluster IS NULL OR :cluster = '' OR i.clusterCode = :cluster OR i.clusterName = :cluster)")
    int deleteByCluster(@Param("cluster") String cluster);
}
