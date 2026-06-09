package com.dga.settings.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Autowired
    private SettingsService settingsService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${dga.notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${dga.notification.mail.from:}")
    private String mailFrom;

    public void sendWecomTest(String username) {
        sendWecomMarkdown("DGA 设置中心测试消息", "由 " + username + " 触发，用于验证企业微信 Webhook 是否可用。");
    }

    public void sendMailTest(String username, String recipient) {
        String to = normalizeEmail(recipient);
        if (to == null) {
            throw new IllegalArgumentException("请填写有效的测试收件邮箱");
        }
        if (!isMailNotificationEnabled()) {
            throw new IllegalStateException("SMTP 邮件通知未启用");
        }
        sendMail("DGA 设置中心测试邮件",
                "这是一封由 " + safe(username) + " 触发的 SMTP 测试邮件。\n\n如果你收到这封邮件，说明系统邮件发送已生效。",
                java.util.Collections.singletonList(to));
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

    public String notifyOffboardingTaskCreated(String username, String cluster, LocalDate departureDate,
                                               List<String> recipients, String taskNo) {
        String content = "用户: " + safe(username)
                + "\n> 集群: " + safe(cluster)
                + "\n> 离职日期: " + (departureDate == null ? "-" : departureDate)
                + "\n> 回收任务编号: " + safe(taskNo)
                + "\n> 通知对象: " + (recipients == null || recipients.isEmpty() ? "待补充" : String.join("，", recipients))
                + "\n> 状态: 已创建任务，系统将在离职日期到达后自动执行权限回收";
        return sendOffboardingNotice("DGA 离职权限回收任务已创建",
                "DGA 离职权限回收任务已创建 - " + safe(username), content, recipients);
    }

    public String notifyOffboardingRevocation(String username, String cluster, LocalDate departureDate,
                                              List<String> recipients, String confirmationNo) {
        String content = "用户: " + safe(username)
                + "\n> 集群: " + safe(cluster)
                + "\n> 离职日期: " + (departureDate == null ? "-" : departureDate)
                + "\n> 确认单编号: " + safe(confirmationNo)
                + "\n> 通知对象: " + (recipients == null || recipients.isEmpty() ? "待补充" : String.join("，", recipients))
                + "\n> 状态: 权限回收流程已执行完成，请核对确认单";
        return sendOffboardingNotice("DGA 离职权限回收已执行",
                "DGA 离职权限回收已执行 - " + safe(username), content, recipients);
    }

    private String sendOffboardingNotice(String title, String subject, String content, List<String> recipients) {
        Map<String, Object> notification = settingsService.getSystemGroup("notification");
        List<String> mailRecipients = extractEmailRecipients(recipients);
        List<String> skippedRecipients = extractSkippedRecipients(recipients);
        List<String> sentChannels = new ArrayList<>();
        List<String> notices = new ArrayList<>();

        if (isMailNotificationEnabled(notification)) {
            if (mailRecipients.isEmpty()) {
                notices.add("未找到可发送邮件地址");
            } else {
                sendMail(subject, mailContent(content), mailRecipients);
                sentChannels.add("邮件(" + String.join("，", mailRecipients) + ")");
            }
        } else {
            notices.add("SMTP 邮件通知未启用");
        }

        if (isEnabled(notification, "wecomEnabled")) {
            try {
                sendWecomMarkdown(title, content);
                sentChannels.add("企业微信");
            } catch (Exception e) {
                notices.add("企业微信通知失败: " + readableMessage(e));
            }
        }

        if (sentChannels.isEmpty()) {
            throw new IllegalStateException(String.join("；", notices));
        }
        if (!skippedRecipients.isEmpty()) {
            notices.add("未发送非邮箱对象: " + String.join("，", skippedRecipients));
        }
        return "已发送通知: " + String.join("，", sentChannels)
                + (notices.isEmpty() ? "" : "；" + String.join("；", notices));
    }

    private void sendMail(String subject, String content, List<String> recipients) {
        if (!isMailNotificationEnabled()) {
            throw new IllegalStateException("SMTP 邮件通知未启用");
        }
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender 未初始化，请检查 spring-boot-starter-mail 和 spring.mail 配置");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("邮件收件人为空");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (trimToNull(mailFrom) != null) {
            message.setFrom(mailFrom.trim());
        }
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
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

    private boolean isMailNotificationEnabled() {
        return isMailNotificationEnabled(settingsService.getSystemGroup("notification"));
    }

    private boolean isMailNotificationEnabled(Map<String, Object> notification) {
        return mailEnabled && isEnabled(notification, "mailEnabled");
    }

    private List<String> extractEmailRecipients(List<String> recipients) {
        List<String> emails = new ArrayList<>();
        if (recipients == null) {
            return emails;
        }
        for (String recipient : recipients) {
            String email = normalizeEmail(recipient);
            if (email != null && !emails.contains(email)) {
                emails.add(email);
            }
        }
        return emails;
    }

    private List<String> extractSkippedRecipients(List<String> recipients) {
        List<String> skipped = new ArrayList<>();
        if (recipients == null) {
            return skipped;
        }
        for (String recipient : recipients) {
            String value = trimToNull(recipient);
            if (value != null && normalizeEmail(value) == null && !skipped.contains(value)) {
                skipped.add(value);
            }
        }
        return skipped;
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return EMAIL_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private String mailContent(String markdownContent) {
        return markdownContent == null ? "" : markdownContent.replace("\n> ", "\n");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private String readableMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "发送失败";
        }
        String message = e.getMessage();
        int nestedIndex = message.indexOf("; nested exception");
        if (nestedIndex > 0) {
            message = message.substring(0, nestedIndex);
        }
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }
}
