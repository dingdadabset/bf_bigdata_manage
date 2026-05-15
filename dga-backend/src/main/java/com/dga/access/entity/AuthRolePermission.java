package com.dga.access.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_role_permission", indexes = {
        @Index(name = "idx_auth_role_perm_role", columnList = "role_code,status"),
        @Index(name = "idx_auth_role_perm_resource", columnList = "cluster_name,database_name,table_name,permission")
})
@EntityListeners(AuditingEntityListener.class)
public class AuthRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 128)
    private String roleCode;

    @Column(name = "cluster_name", nullable = false, length = 255)
    private String cluster;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "database_name", length = 255)
    private String databaseName;

    @Column(name = "table_name", length = 255)
    private String tableName;

    @Column(name = "permission", nullable = false, length = 50)
    private String permission;

    @Column(name = "auth_backend", length = 50)
    private String authBackend;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "create_time")
    @CreatedDate
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;
}
