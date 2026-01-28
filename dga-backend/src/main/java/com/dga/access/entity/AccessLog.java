package com.dga.access.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dga_access_log")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "request_type", nullable = false)
    private String requestType; // GRANT_HIVE, CREATE_LDAP_USER

    @Column(name = "target_resource")
    private String targetResource; // e.g., database name

    @Column(name = "permission_granted")
    private String permissionGranted;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
