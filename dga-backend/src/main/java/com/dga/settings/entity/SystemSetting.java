package com.dga.settings.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dga_system_setting",
        uniqueConstraints = @UniqueConstraint(name = "uk_setting_scope_group_key", columnNames = {"scope", "setting_group", "setting_key"}),
        indexes = {
                @Index(name = "idx_setting_scope_group", columnList = "scope,setting_group"),
                @Index(name = "idx_setting_key", columnList = "setting_key")
        })
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope", nullable = false, length = 100)
    private String scope;

    @Column(name = "setting_group", nullable = false, length = 100)
    private String settingGroup;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Lob
    @Column(name = "setting_value")
    private String settingValue;

    @Column(name = "value_type", length = 30)
    private String valueType;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSettingGroup() {
        return settingGroup;
    }

    public void setSettingGroup(String settingGroup) {
        this.settingGroup = settingGroup;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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
