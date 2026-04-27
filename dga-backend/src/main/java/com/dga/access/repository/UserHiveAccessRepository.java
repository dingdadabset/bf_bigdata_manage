package com.dga.access.repository;

import com.dga.access.entity.UserHiveAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserHiveAccessRepository extends JpaRepository<UserHiveAccess, Long> {
    List<UserHiveAccess> findByUsername(String username);
    List<UserHiveAccess> findByUsernameAndStatus(String username, String status);
    List<UserHiveAccess> findByUsernameAndClusterName(String username, String clusterName);
    List<UserHiveAccess> findByUsernameAndClusterNameAndStatus(String username, String clusterName, String status);
    List<UserHiveAccess> findByUsernameAndIsDeletedFalse(String username);
    List<UserHiveAccess> findByUsernameAndStatusAndIsDeletedFalse(String username, String status);
    List<UserHiveAccess> findByUsernameAndClusterNameAndIsDeletedFalse(String username, String clusterName);
    List<UserHiveAccess> findByUsernameAndClusterNameAndStatusAndIsDeletedFalse(String username, String clusterName, String status);
    List<UserHiveAccess> findByUsernameAndStatusAndIsDeletedTrue(String username, String status);
    List<UserHiveAccess> findByUsernameAndClusterNameAndStatusAndIsDeletedTrue(String username, String clusterName, String status);

    @Modifying
    @Transactional
    @Query("UPDATE UserHiveAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.isDeleted = false")
    int softDeleteAllAccessByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE UserHiveAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.clusterName = ?2 AND u.isDeleted = false")
    int softDeleteAllAccessByUsernameAndClusterName(String username, String clusterName);

    @Modifying
    @Transactional
    @Query("UPDATE UserHiveAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.clusterName = ?2 AND u.databaseName = ?3 AND u.tableName IS NULL AND u.permission = ?4 AND u.isDeleted = false")
    int softDeleteDatabaseAccess(String username, String clusterName, String databaseName, String permission);

    @Modifying
    @Transactional
    @Query("UPDATE UserHiveAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.clusterName = ?2 AND u.databaseName = ?3 AND u.tableName = ?4 AND u.permission = ?5 AND u.isDeleted = false")
    int softDeleteTableAccess(String username, String clusterName, String databaseName, String tableName, String permission);
}
