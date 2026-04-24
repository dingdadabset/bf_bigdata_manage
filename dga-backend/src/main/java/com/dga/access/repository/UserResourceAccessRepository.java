package com.dga.access.repository;

import com.dga.access.entity.UserResourceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserResourceAccessRepository extends JpaRepository<UserResourceAccess, Long> {
    List<UserResourceAccess> findByUsernameAndIsDeletedFalse(String username);
    List<UserResourceAccess> findByUsernameAndClusterCodeAndIsDeletedFalse(String username, String clusterCode);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.isDeleted = false")
    int softDeleteAllByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName IS NULL AND u.permission = ?4 AND u.isDeleted = false")
    int softDeleteDatabaseAccess(String username, String clusterCodeOrName, String databaseName, String permission);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName = ?4 AND u.permission = ?5 AND u.isDeleted = false")
    int softDeleteTableAccess(String username, String clusterCodeOrName, String databaseName, String tableName, String permission);
}
