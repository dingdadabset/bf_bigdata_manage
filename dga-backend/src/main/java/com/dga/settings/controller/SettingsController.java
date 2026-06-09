package com.dga.settings.controller;

import com.dga.access.security.CurrentUser;
import com.dga.access.service.AdminGuard;
import com.dga.settings.service.NotificationService;
import com.dga.settings.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping
    public Map<String, Map<String, Object>> getSettings() {
        return settingsService.getSystemSettings(true);
    }

    @PutMapping("/{group}")
    public Map<String, Map<String, Object>> updateSettings(@PathVariable String group,
                                                           @RequestBody Map<String, Object> values,
                                                           HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级管理员可修改系统设置");
        return settingsService.updateSystemGroup(group, values, CurrentUser.usernameOrUnknown());
    }

    @GetMapping("/me")
    public Map<String, Object> getMySettings() {
        return settingsService.getPersonalSettings(CurrentUser.usernameOrUnknown());
    }

    @PutMapping("/me")
    public Map<String, Object> updateMySettings(@RequestBody Map<String, Object> values) {
        return settingsService.updatePersonalSettings(CurrentUser.usernameOrUnknown(), values);
    }

    @PostMapping("/notifications/wecom/test")
    public Map<String, Object> testWecom(HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级管理员可测试通知告警");
        notificationService.sendWecomTest(CurrentUser.usernameOrUnknown());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "企业微信测试消息已发送");
        return result;
    }

    @PostMapping("/notifications/mail/test")
    public Map<String, Object> testMail(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级管理员可测试通知告警");
        String recipient = body == null ? null : String.valueOf(body.getOrDefault("recipient", ""));
        notificationService.sendMailTest(CurrentUser.usernameOrUnknown(), recipient);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "SMTP 测试邮件已发送");
        return result;
    }
}
