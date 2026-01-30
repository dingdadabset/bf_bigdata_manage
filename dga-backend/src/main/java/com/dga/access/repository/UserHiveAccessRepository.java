package com.dga.access.repository;

import com.dga.access.entity.UserHiveAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHiveAccessRepository extends JpaRepository<UserHiveAccess, Long> {
    List<UserHiveAccess> findByUsername(String username);
    List<UserHiveAccess> findByUsernameAndStatus(String username, String status);
    List<UserHiveAccess> findByUsernameAndClusterName(String username, String clusterName);
    List<UserHiveAccess> findByUsernameAndClusterNameAndStatus(String username, String clusterName, String status);
    List<UserHiveAccess> findByUsernameAndIsDeletedFalse(String username);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserHiveAccess u WHERE u.username = ?1")
    void revokeAllAccessByUsername(String username);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserHiveAccess u WHERE u.username = ?1 AND u.clusterName = ?2 AND u.databaseName = ?3 AND u.tableName IS NULL AND u.permission = ?4")
    void revokeDatabaseAccess(String username, String clusterName, String databaseName, String permission);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserHiveAccess u WHERE u.username = ?1 AND u.clusterName = ?2 AND u.databaseName = ?3 AND u.tableName = ?4 AND u.permission = ?5")
    void revokeTableAccess(String username, String clusterName, String databaseName, String tableName, String permission);
}
