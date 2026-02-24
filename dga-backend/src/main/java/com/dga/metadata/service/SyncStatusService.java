package com.dga.metadata.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

@Service
public class SyncStatusService {
    // Key: "global" or specific dataSourceId
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();
    private final Map<String, String> messageMap = new ConcurrentHashMap<>();

    public void setStatus(String key, String status, String message) {
        statusMap.put(key, status);
        if (message != null) {
            messageMap.put(key, message);
        }
    }

    public String getStatus(String key) {
        return statusMap.getOrDefault(key, "IDLE");
    }
    
    public String getMessage(String key) {
        return messageMap.getOrDefault(key, "");
    }
    
    public Map<String, Object> getFullStatus(String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", getStatus(key));
        result.put("message", getMessage(key));
        return result;
    }
}
