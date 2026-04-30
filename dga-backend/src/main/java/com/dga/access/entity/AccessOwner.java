package com.dga.access.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_owner",
        indexes = {
                @Index(name = "idx_access_owner_code", columnList = "owner_code"),
                @Index(name = "idx_access_owner_status", columnList = "status")
        })
public class AccessOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_code", nullable = false, length = 100)
    private String ownerCode;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        }
        if (source == null || source.trim().isEmpty()) {
            source = "MANUAL";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerCode() {
        return ownerCode;
    }

    public void setOwnerCode(String ownerCode) {
        this.ownerCode = ownerCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
