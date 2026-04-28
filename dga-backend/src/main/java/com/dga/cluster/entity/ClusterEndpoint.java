package com.dga.cluster.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_cluster_endpoint")
public class ClusterEndpoint {

    public static final String TYPE_HIVE_SERVER2 = "HIVE_SERVER2";
    public static final String TYPE_HIVE_METASTORE_DB = "HIVE_METASTORE_DB";
    public static final String TYPE_STARROCKS_JDBC = "STARROCKS_JDBC";
    public static final String TYPE_DORIS_JDBC = "DORIS_JDBC";
    public static final String TYPE_AZKABAN_DB = "AZKABAN_DB";
    public static final String TYPE_DOLPHINSCHEDULER_DB = "DOLPHINSCHEDULER_DB";
    public static final String TYPE_LDAP = "LDAP";
    public static final String TYPE_RANGER = "RANGER";

    public static final String AUTH_SENTRY = "SENTRY";
    public static final String AUTH_STARROCKS_SQL = "STARROCKS_SQL";
    public static final String AUTH_DORIS_SQL = "DORIS_SQL";
    public static final String AUTH_RANGER = "RANGER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cluster_code", nullable = false)
    private String clusterCode;

    @Column(name = "endpoint_type", nullable = false)
    private String endpointType;

    @Column(name = "auth_backend")
    private String authBackend;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "username")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    private String password;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "base_dn")
    private String baseDn;

    @Column(name = "user_base_dn")
    private String userBaseDn;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public String getAuthBackend() {
        return authBackend;
    }

    public void setAuthBackend(String authBackend) {
        this.authBackend = authBackend;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
