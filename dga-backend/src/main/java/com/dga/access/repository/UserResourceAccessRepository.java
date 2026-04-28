package com.dga.access.repository;

import com.dga.access.entity.UserResourceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserResourceAccessRepository extends JpaRepository<UserResourceAccess, Long> {
    List<UserResourceAccess> findByUsernameAndIsDeletedFalse(String username);
    List<UserResourceAccess> findByUsernameAndClusterCodeAndIsDeletedFalse(String username, String clusterCode);

    @Query("SELECT u FROM UserResourceAccess u WHERE u.isDeleted = false AND u.status = 'ACTIVE' " +
            "AND (:clusterCode IS NULL OR u.clusterCode = :clusterCode) " +
            "AND u.databaseName = :databaseName " +
            "AND (u.tableName = :tableName OR u.tableName IS NULL) " +
            "ORDER BY u.username ASC, u.resourceType ASC, u.permission ASC")
    List<UserResourceAccess> findActivePermissionsForTable(@Param("clusterCode") String clusterCode,
                                                           @Param("databaseName") String databaseName,
                                                           @Param("tableName") String tableName);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.isDeleted = false")
    int softDeleteAllByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.isDeleted = false")
    int softDeleteAllByUsernameAndCluster(String username, String clusterCodeOrName);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName IS NULL AND u.permission = ?4 AND u.isDeleted = false")
    int softDeleteDatabaseAccess(String username, String clusterCodeOrName, String databaseName, String permission);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName = ?4 AND u.permission = ?5 AND u.isDeleted = false")
    int softDeleteTableAccess(String username, String clusterCodeOrName, String databaseName, String tableName, String permission);
}
