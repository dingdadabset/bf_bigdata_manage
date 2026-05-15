package com.dga.access.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_user_role", indexes = {
        @Index(name = "idx_auth_user_role_role", columnList = "role_code,status"),
        @Index(name = "idx_auth_user_role_subject", columnList = "subject_type,subject_name,status")
})
@EntityListeners(AuditingEntityListener.class)
public class AuthUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 128)
    private String roleCode;

    @Column(name = "cluster_name", nullable = false, length = 255)
    private String cluster;

    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name = "subject_name", nullable = false, length = 200)
    private String subjectName;

    @Column(name = "auth_backend", length = 50)
    private String authBackend;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "backend_sync_status", length = 40)
    private String backendSyncStatus;

    @Column(name = "sync_message", length = 1000)
    private String syncMessage;

    @Column(name = "expanded_users", length = 2000)
    private String expandedUsers;

    @Column(name = "create_time")
    @CreatedDate
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @LastModifiedDate
    private LocalDateTime updateTime;
}
