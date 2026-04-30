package com.dga.access.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "dga_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_cluster_username", columnNames = {"cluster_name", "username"}))
@EntityListeners(AuditingEntityListener.class)
public class DgaUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String password;

    private String email;
    
    private String firstName;
    
    private String lastName;

    @Column(name = "creation_strategy")
    private String creationStrategy; // LDAP, IPA_SSH, IPA_HTTP

    @Column(name = "user_type", length = 30)
    private String userType = "INTERNAL"; // INTERNAL, OUTSOURCER, TEMPORARY, SERVICE

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "last_active_source", length = 50)
    private String lastActiveSource;

    @Column(name = "cluster_name")
    private String clusterName;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    @Column(name = "is_protected")
    private Boolean protectedUser;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCreationStrategy() {
        return creationStrategy;
    }

    public void setCreationStrategy(String creationStrategy) {
        this.creationStrategy = creationStrategy;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getLastActiveSource() {
        return lastActiveSource;
    }

    public void setLastActiveSource(String lastActiveSource) {
        this.lastActiveSource = lastActiveSource;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public Boolean getProtectedUser() {
        return protectedUser;
    }

    public void setProtectedUser(Boolean protectedUser) {
        this.protectedUser = protectedUser;
    }

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (userType == null || userType.trim().isEmpty()) {
            userType = "INTERNAL";
        }
    }

    @Override
    public String toString() {
        return "DgaUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", creationStrategy='" + creationStrategy + '\'' +
                ", createTime=" + createTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DgaUser dgaUser = (DgaUser) o;
        return Objects.equals(id, dgaUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
