package com.dga.access.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auth_role_assignment_audit", indexes = {
        @Index(name = "idx_auth_role_audit_role", columnList = "role_code,action_time"),
        @Index(name = "idx_auth_role_audit_subject", columnList = "subject_type,subject_name")
})
@EntityListeners(AuditingEntityListener.class)
public class AuthRoleAssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", length = 128)
    private String roleCode;

    @Column(name = "cluster_name", length = 255)
    private String cluster;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "subject_type", length = 20)
    private String subjectType;

    @Column(name = "subject_name", length = 200)
    private String subjectName;

    @Column(name = "resource_summary", length = 1000)
    private String resourceSummary;

    @Column(name = "backend_status", length = 40)
    private String backendStatus;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "action_time")
    @CreatedDate
    private LocalDateTime actionTime;
}
