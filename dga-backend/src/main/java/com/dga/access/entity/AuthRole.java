package com.dga.access.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_role", indexes = {
        @Index(name = "idx_auth_role_cluster", columnList = "cluster_name,status"),
        @Index(name = "idx_auth_role_code", columnList = "role_code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class AuthRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 128, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 200)
    private String roleName;

    @Column(name = "cluster_name", nullable = false, length = 255)
    private String cluster;

    @Column(name = "engine_type", length = 50)
    private String engineType;

    @Column(name = "auth_backend", length = 50)
    private String authBackend;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "create_time")
    @CreatedDate
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;
}
