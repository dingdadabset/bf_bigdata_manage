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

    @Query("SELECT u FROM UserResourceAccess u WHERE u.username = :username " +
            "AND (:cluster IS NULL OR :cluster = '' OR u.clusterCode = :cluster OR u.clusterName = :cluster) " +
            "ORDER BY COALESCE(u.revokeTime, u.grantTime, u.updateTime) DESC")
    List<UserResourceAccess> findAuditByUsernameAndCluster(@Param("username") String username,
                                                           @Param("cluster") String cluster);

    @Query("SELECT u FROM UserResourceAccess u WHERE u.isDeleted = false AND u.status = 'ACTIVE' " +
            "AND (:cluster IS NULL OR :cluster = '' OR u.clusterCode = :cluster OR u.clusterName = :cluster)")
    List<UserResourceAccess> findActiveByCluster(@Param("cluster") String cluster);

    @Query("SELECT COUNT(u) FROM UserResourceAccess u WHERE u.isDeleted = false AND u.status = 'ACTIVE' " +
            "AND u.username = :username " +
            "AND (:cluster IS NULL OR :cluster = '' OR u.clusterCode = :cluster OR u.clusterName = :cluster)")
    long countActiveByUsernameAndCluster(@Param("username") String username,
                                         @Param("cluster") String cluster);

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
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = ?2, u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND u.isDeleted = false")
    int softDeleteAllByUsername(String username, String revokedBy);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = ?3, u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.isDeleted = false")
    int softDeleteAllByUsernameAndCluster(String username, String clusterCodeOrName, String revokedBy);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = ?5, u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName IS NULL AND u.permission = ?4 AND u.isDeleted = false")
    int softDeleteDatabaseAccess(String username, String clusterCodeOrName, String databaseName, String permission, String revokedBy);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = ?6, u.revokeTime = CURRENT_TIMESTAMP WHERE u.username = ?1 AND (u.clusterCode = ?2 OR u.clusterName = ?2) AND u.databaseName = ?3 AND u.tableName = ?4 AND u.permission = ?5 AND u.isDeleted = false")
    int softDeleteTableAccess(String username, String clusterCodeOrName, String databaseName, String tableName, String permission, String revokedBy);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = :revokedBy, u.revokeTime = CURRENT_TIMESTAMP " +
            "WHERE u.isDeleted = false AND (u.clusterCode = :cluster OR u.clusterName = :cluster) " +
            "AND u.databaseName = :databaseName AND u.tableName IS NULL AND u.permission = :permission " +
            "AND u.grantMode = 'ROLE' AND u.source = 'RBAC_ROLE_SUBSET' " +
            "AND u.roleCode = :roleCode AND u.subjectType = :subjectType AND u.subjectName = :subjectName")
    int softDeleteRoleSubsetDatabaseAccess(@Param("cluster") String cluster,
                                           @Param("databaseName") String databaseName,
                                           @Param("permission") String permission,
                                           @Param("roleCode") String roleCode,
                                           @Param("subjectType") String subjectType,
                                           @Param("subjectName") String subjectName,
                                           @Param("revokedBy") String revokedBy);

    @Modifying
    @Transactional
    @Query("UPDATE UserResourceAccess u SET u.isDeleted = true, u.status = 'REVOKED', u.revokedBy = :revokedBy, u.revokeTime = CURRENT_TIMESTAMP " +
            "WHERE u.isDeleted = false AND (u.clusterCode = :cluster OR u.clusterName = :cluster) " +
            "AND u.databaseName = :databaseName AND u.tableName = :tableName AND u.permission = :permission " +
            "AND u.grantMode = 'ROLE' AND u.source = 'RBAC_ROLE_SUBSET' " +
            "AND u.roleCode = :roleCode AND u.subjectType = :subjectType AND u.subjectName = :subjectName")
    int softDeleteRoleSubsetTableAccess(@Param("cluster") String cluster,
                                        @Param("databaseName") String databaseName,
                                        @Param("tableName") String tableName,
                                        @Param("permission") String permission,
                                        @Param("roleCode") String roleCode,
                                        @Param("subjectType") String subjectType,
                                        @Param("subjectName") String subjectName,
                                        @Param("revokedBy") String revokedBy);

    @Query("SELECT u FROM UserResourceAccess u WHERE u.isDeleted = false AND u.status = 'ACTIVE' " +
            "AND u.username = :username " +
            "AND (:cluster IS NULL OR u.clusterCode = :cluster OR u.clusterName = :cluster) " +
            "AND (:authBackend IS NULL OR u.authBackend = :authBackend) " +
            "AND u.roleCode = :roleCode AND u.grantMode = 'ROLE_ADOPTION'")
    List<UserResourceAccess> findActiveRoleAdoptionRecords(@Param("username") String username,
                                                           @Param("cluster") String cluster,
                                                           @Param("authBackend") String authBackend,
                                                           @Param("roleCode") String roleCode);
}
