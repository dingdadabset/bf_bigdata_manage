package com.dga.settings.service;

import com.dga.settings.entity.SystemSetting;
import com.dga.settings.repository.SystemSettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SettingsService {

    public static final String SYSTEM_SCOPE = "SYSTEM";
    private static final String USER_SCOPE_PREFIX = "USER:";

    private static final String TYPE_STRING = "STRING";
    private static final String TYPE_NUMBER = "NUMBER";
    private static final String TYPE_BOOLEAN = "BOOLEAN";
    private static final String TYPE_JSON = "JSON";

    private static final String GROUP_SYSTEM = "system";
    private static final String GROUP_DATA_MAP = "dataMap";
    private static final String GROUP_METADATA_COLLECTION = "metadataCollection";
    private static final String GROUP_NOTIFICATION = "notification";
    private static final String GROUP_PERSONAL = "personal";

    @Autowired
    private SystemSettingRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Map<String, Object>> getSystemSettings(boolean maskSensitive) {
        Map<String, Map<String, Object>> result = defaultSystemSettings();
        for (SystemSetting setting : repository.findByScope(SYSTEM_SCOPE)) {
            Map<String, Object> group = result.computeIfAbsent(setting.getSettingGroup(), key -> new LinkedHashMap<>());
            group.put(setting.getSettingKey(), decode(setting));
        }
        if (maskSensitive) {
            maskNotification(result.get(GROUP_NOTIFICATION));
        }
        return result;
    }

    public Map<String, Object> getSystemGroup(String group) {
        Map<String, Map<String, Object>> all = getSystemSettings(false);
        return all.getOrDefault(group, new LinkedHashMap<>());
    }

    public String getString(String group, String key) {
        Object value = getSystemGroup(group).get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public int getInt(String group, String key, int defaultValue) {
        Object value = getSystemGroup(group).get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String group, String key, boolean defaultValue) {
        Object value = getSystemGroup(group).get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public Map<String, Object> getPersonalSettings(String username) {
        Map<String, Object> result = defaultPersonalSettings();
        for (SystemSetting setting : repository.findByScope(userScope(username))) {
            if (GROUP_PERSONAL.equals(setting.getSettingGroup())) {
                result.put(setting.getSettingKey(), decode(setting));
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updatePersonalSettings(String username, Map<String, Object> values) {
        Map<String, Object> defaults = defaultPersonalSettings();
        Map<String, Object> merged = new LinkedHashMap<>(defaults);
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (defaults.containsKey(entry.getKey())) {
                    merged.put(entry.getKey(), normalizeValue(defaults.get(entry.getKey()), entry.getValue()));
                }
            }
        }
        saveGroup(userScope(username), GROUP_PERSONAL, merged, username);
        return getPersonalSettings(username);
    }

    @Transactional
    public Map<String, Map<String, Object>> updateSystemGroup(String group, Map<String, Object> values, String username) {
        Map<String, Map<String, Object>> defaults = defaultSystemSettings();
        if (!defaults.containsKey(group)) {
            throw new IllegalArgumentException("不支持的设置分组: " + group);
        }
        Map<String, Object> merged = new LinkedHashMap<>(getSystemGroup(group));
        Map<String, Object> groupDefaults = defaults.get(group);
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!groupDefaults.containsKey(entry.getKey())) {
                    continue;
                }
                if (GROUP_NOTIFICATION.equals(group)
                        && "wecomWebhook".equals(entry.getKey())
                        && isMaskedValue(entry.getValue())) {
                    continue;
                }
                merged.put(entry.getKey(), normalizeValue(groupDefaults.get(entry.getKey()), entry.getValue()));
            }
        }
        saveGroup(SYSTEM_SCOPE, group, merged, username);
        return getSystemSettings(true);
    }

    private void saveGroup(String scope, String group, Map<String, Object> values, String username) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Optional<SystemSetting> optional = repository.findByScopeAndSettingGroupAndSettingKey(scope, group, entry.getKey());
            SystemSetting setting = optional.orElseGet(SystemSetting::new);
            setting.setScope(scope);
            setting.setSettingGroup(group);
            setting.setSettingKey(entry.getKey());
            setting.setValueType(resolveType(entry.getValue()));
            setting.setSettingValue(encode(entry.getValue(), setting.getValueType()));
            setting.setUpdatedBy(username);
            repository.save(setting);
        }
    }

    private Map<String, Map<String, Object>> defaultSystemSettings() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("systemName", "DGA Platform");
        system.put("footerText", "DGA Platform ©2026 Created by Data Engineering Team");
        system.put("maintenanceNotice", "");
        system.put("aiSearchEnabled", false);
        system.put("defaultPageSize", 10);
        result.put(GROUP_SYSTEM, system);

        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("hotKeywords", new ArrayList<>(Arrays.asList("user", "order", "payment")));
        dataMap.put("defaultSearchScope", "ALL");
        dataMap.put("recentRetentionDays", 30);
        dataMap.put("resultSort", "RELEVANCE");
        dataMap.put("showSensitiveFields", false);
        dataMap.put("defaultPageSize", 10);
        result.put(GROUP_DATA_MAP, dataMap);

        Map<String, Object> metadataCollection = new LinkedHashMap<>();
        metadataCollection.put("collectCron", "0 0 2 * * ?");
        metadataCollection.put("partitionLatestLimit", 200);
        metadataCollection.put("retryTimes", 1);
        metadataCollection.put("autoSyncDataSources", true);
        result.put(GROUP_METADATA_COLLECTION, metadataCollection);

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("wecomEnabled", false);
        notification.put("wecomWebhook", "");
        notification.put("collectFailureAlert", true);
        notification.put("qualityIssueAlert", true);
        result.put(GROUP_NOTIFICATION, notification);

        return result;
    }

    private Map<String, Object> defaultPersonalSettings() {
        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("defaultHome", "/datamap");
        personal.put("tableDensity", "middle");
        personal.put("defaultPageSize", 10);
        personal.put("showHints", true);
        return personal;
    }

    private Object normalizeValue(Object defaultValue, Object value) {
        if (defaultValue instanceof Boolean) {
            if (value instanceof Boolean) return value;
            return Boolean.parseBoolean(String.valueOf(value));
        }
        if (defaultValue instanceof Number) {
            if (value instanceof Number) return ((Number) value).intValue();
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception e) {
                return defaultValue;
            }
        }
        if (defaultValue instanceof Collection) {
            if (value instanceof Collection) return new ArrayList<>((Collection<?>) value);
            if (value == null || String.valueOf(value).trim().isEmpty()) return new ArrayList<>();
            return new ArrayList<>(Arrays.asList(String.valueOf(value).split("\\s*,\\s*")));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String resolveType(Object value) {
        if (value instanceof Boolean) return TYPE_BOOLEAN;
        if (value instanceof Number) return TYPE_NUMBER;
        if (value instanceof Collection || value instanceof Map) return TYPE_JSON;
        return TYPE_STRING;
    }

    private String encode(Object value, String type) {
        if (TYPE_JSON.equals(type)) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                return "[]";
            }
        }
        return value == null ? "" : String.valueOf(value);
    }

    private Object decode(SystemSetting setting) {
        String value = setting.getSettingValue();
        if (TYPE_BOOLEAN.equals(setting.getValueType())) {
            return Boolean.parseBoolean(value);
        }
        if (TYPE_NUMBER.equals(setting.getValueType())) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return 0;
            }
        }
        if (TYPE_JSON.equals(setting.getValueType())) {
            try {
                return objectMapper.readValue(value, new TypeReference<Object>() {});
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return value == null ? "" : value;
    }

    private void maskNotification(Map<String, Object> notification) {
        if (notification == null) {
            return;
        }
        Object webhook = notification.get("wecomWebhook");
        if (webhook != null && !String.valueOf(webhook).trim().isEmpty()) {
            notification.put("wecomWebhook", maskWebhook(String.valueOf(webhook)));
        }
    }

    private String maskWebhook(String webhook) {
        if (webhook.length() <= 12) {
            return "****";
        }
        if (webhook.length() <= 30) {
            return webhook.substring(0, 6) + "****" + webhook.substring(webhook.length() - 4);
        }
        return webhook.substring(0, 24) + "****" + webhook.substring(webhook.length() - 6);
    }

    private boolean isMaskedValue(Object value) {
        return value != null && String.valueOf(value).contains("****");
    }

    private String userScope(String username) {
        return USER_SCOPE_PREFIX + (username == null || username.trim().isEmpty() ? "unknown" : username.trim());
    }
}
