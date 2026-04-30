package com.dga.settings.repository;

import com.dga.settings.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    List<SystemSetting> findByScope(String scope);
    List<SystemSetting> findByScopeAndSettingGroup(String scope, String settingGroup);
    Optional<SystemSetting> findByScopeAndSettingGroupAndSettingKey(String scope, String settingGroup, String settingKey);
}
