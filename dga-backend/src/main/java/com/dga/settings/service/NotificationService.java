package com.dga.settings.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SettingsService settingsService;

    public void sendWecomTest(String username) {
        sendWecomMarkdown("DGA 设置中心测试消息", "由 " + username + " 触发，用于验证企业微信 Webhook 是否可用。");
    }

    public void notifyCollectionFailure(String message) {
        Map<String, Object> notification = settingsService.getSystemGroup("notification");
        if (!isEnabled(notification, "wecomEnabled") || !isEnabled(notification, "collectFailureAlert")) {
            return;
        }
        try {
            sendWecomMarkdown("DGA 元数据采集失败", message);
        } catch (Exception ignored) {
            // 告警失败不能影响主任务状态。
        }
    }

    public void notifyQualityIssue(String message) {
        Map<String, Object> notification = settingsService.getSystemGroup("notification");
        if (!isEnabled(notification, "wecomEnabled") || !isEnabled(notification, "qualityIssueAlert")) {
            return;
        }
        try {
            sendWecomMarkdown("DGA 数据质量异常", message);
        } catch (Exception ignored) {
            // 告警失败不能影响质量任务状态。
        }
    }

    private void sendWecomMarkdown(String title, String content) {
        Map<String, Object> notification = settingsService.getSystemGroup("notification");
        String webhook = String.valueOf(notification.getOrDefault("wecomWebhook", "")).trim();
        if (webhook.isEmpty()) {
            throw new IllegalStateException("企业微信 Webhook 未配置");
        }

        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", "**" + title + "**\n> " + content);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(webhook, new HttpEntity<>(payload, headers), Map.class);
        Map body = response.getBody();
        Object errcode = body == null ? null : body.get("errcode");
        if (errcode != null && !"0".equals(String.valueOf(errcode))) {
            Object errmsg = body.get("errmsg");
            throw new IllegalStateException("企业微信返回失败: " + (errmsg == null ? errcode : errmsg));
        }
    }

    private boolean isEnabled(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }
}
