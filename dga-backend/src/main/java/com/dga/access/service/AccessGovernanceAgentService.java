package com.dga.access.service;

import com.dga.access.config.AgentLlmProperties;
import com.dga.access.dto.GovernanceAgentRequest;
import com.dga.access.entity.AccessGovernanceAiAdvice;
import com.dga.access.entity.AccessGovernanceAiPlan;
import com.dga.access.entity.AccessGovernanceIssue;
import com.dga.access.repository.AccessGovernanceAiAdviceRepository;
import com.dga.access.repository.AccessGovernanceAiPlanRepository;
import com.dga.access.repository.AccessGovernanceIssueRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccessGovernanceAgentService {

    @Autowired
    private AgentLlmProperties properties;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private AccessGovernanceIssueRepository issueRepository;

    @Autowired
    private AccessGovernanceAiAdviceRepository adviceRepository;

    @Autowired
    private AccessGovernanceAiPlanRepository planRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> configStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", true);
        status.put("provider", properties.getProvider());
        status.put("model", properties.getModel());
        status.put("configured", properties.isConfigured());
        return status;
    }

    public Map<String, Object> analyze(GovernanceAgentRequest request, String operator) {
        Long issueId = request.getIssueId();
        if (issueId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "issueId 不能为空");
        }
        AccessGovernanceIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "风险记录不存在"));
        Map<String, Object> context = issueContext(issue);
        Map<String, Object> advice = callOrFallback("ANALYZE", Collections.singletonList(context));
        advice.put("issueId", issue.getId());
        normalizeAdvice(advice, issue);
        saveAdvice(issue, advice, context, operator);
        return advice;
    }

    public Map<String, Object> plan(GovernanceAgentRequest request, String operator) {
        List<AccessGovernanceIssue> issues = loadIssues(request);
        if (issues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择至少一条风险记录");
        }
        List<Map<String, Object>> contexts = issues.stream().map(this::issueContext).collect(Collectors.toList());
        Map<String, Object> plan = callOrFallback("PLAN", contexts);
        plan.putIfAbsent("summary", fallbackPlanSummary(issues));
        plan.putIfAbsent("actions", fallbackActions(issues));
        AccessGovernanceAiPlan entity = new AccessGovernanceAiPlan();
        entity.setCluster(firstNonBlank(request.getCluster(), inferCluster(issues)));
        entity.setScopeType(firstNonBlank(request.getScope(), "SELECTED"));
        entity.setSummary(String.valueOf(plan.get("summary")));
        entity.setPlanJson(writeJson(plan));
        entity.setStatus("DRAFT");
        entity.setCreatedBy(operator);
        planRepository.save(entity);
        plan.put("planId", entity.getId());
        return plan;
    }

    public Map<String, Object> report(GovernanceAgentRequest request) {
        List<AccessGovernanceIssue> issues = loadIssues(request);
        if (issues.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("markdown", "## 权限风险治理报告\n\n当前筛选范围内暂无风险记录。");
            return empty;
        }
        List<Map<String, Object>> contexts = issues.stream().limit(80).map(this::issueContext).collect(Collectors.toList());
        Map<String, Object> result = callOrFallback("REPORT", contexts);
        String markdown = String.valueOf(result.getOrDefault("markdown", fallbackReport(issues)));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("markdown", markdown);
        return response;
    }

    private Map<String, Object> callOrFallback(String mode, List<Map<String, Object>> contexts) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeepSeek API Key 未配置，请先设置 DEEPSEEK_API_KEY 后再生成 AI 建议");
        }
        String systemPrompt = systemPrompt(mode);
        String userPrompt = writeJson(contexts);
        String text = deepSeekClient.chat(systemPrompt, userPrompt);
        return parseJsonObject(text);
    }

    private String systemPrompt(String mode) {
        if ("REPORT".equals(mode)) {
            return "你是 DGA 数据权限风险治理助手。根据输入风险记录生成 Markdown 治理报告，输出 JSON：{\"markdown\":\"...\"}。不要输出额外解释。";
        }
        if ("PLAN".equals(mode)) {
            return "你是 DGA 数据权限风险治理助手。请根据风险列表生成批量治理计划，输出 JSON，字段包含 summary 和 actions。actions 内 type 只能是 REVOKE、DOWNGRADE、ASSIGN_OWNER、KEEP_WITH_REASON、CLOSE_FALSE_POSITIVE、REVIEW_MANUALLY。AI 只提供建议，不直接执行。";
        }
        return "你是 DGA 数据权限风险治理助手。请分析单条风险并输出 JSON，字段包含 summary、riskReason、suggestedAction、suggestedOwner、confidence、approvalRequired、steps、evidence。suggestedAction 只能是 REVOKE、DOWNGRADE、ASSIGN_OWNER、KEEP_WITH_REASON、CLOSE_FALSE_POSITIVE、REVIEW_MANUALLY。AI 只提供建议，不直接执行。";
    }

    private Map<String, Object> parseJsonObject(String text) {
        try {
            String json = extractJson(text);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容不是合法 JSON");
        }
    }

    private String extractJson(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private void normalizeAdvice(Map<String, Object> advice, AccessGovernanceIssue issue) {
        advice.putIfAbsent("summary", riskTypeLabel(issue.getIssueType()) + "：" + firstNonBlank(issue.getUsername(), "未知账号"));
        advice.putIfAbsent("riskReason", firstNonBlank(firstNonBlank(issue.getEvidence(), issue.getRecommendation()), "该风险需要管理员复核"));
        advice.putIfAbsent("suggestedAction", fallbackAction(issue));
        advice.putIfAbsent("suggestedOwner", firstNonBlank(issue.getOwner(), ""));
        advice.putIfAbsent("confidence", 0.68);
        advice.putIfAbsent("approvalRequired", true);
        advice.putIfAbsent("steps", Arrays.asList("核对风险上下文", "确认负责人或审批依据", "管理员确认后执行治理动作"));
        advice.putIfAbsent("evidence", evidence(issue));
    }

    private void saveAdvice(AccessGovernanceIssue issue, Map<String, Object> advice, Map<String, Object> context, String operator) {
        AccessGovernanceAiAdvice entity = new AccessGovernanceAiAdvice();
        entity.setIssueId(issue.getId());
        entity.setProvider(properties.getProvider());
        entity.setModel(properties.getModel());
        entity.setSummary(limit(String.valueOf(advice.get("summary")), 1000));
        entity.setRiskReason(limit(String.valueOf(advice.get("riskReason")), 2000));
        entity.setSuggestedAction(limit(String.valueOf(advice.get("suggestedAction")), 50));
        entity.setSuggestedOwner(limit(String.valueOf(advice.getOrDefault("suggestedOwner", "")), 100));
        entity.setConfidence(number(advice.get("confidence"), 0.68));
        entity.setApprovalRequired(Boolean.TRUE.equals(advice.get("approvalRequired")) || "true".equalsIgnoreCase(String.valueOf(advice.get("approvalRequired"))));
        entity.setAdviceJson(writeJson(advice));
        entity.setPromptHash(hash(writeJson(context)));
        entity.setCreatedBy(operator);
        adviceRepository.save(entity);
        advice.put("adviceId", entity.getId());
    }

    private List<AccessGovernanceIssue> loadIssues(GovernanceAgentRequest request) {
        if (request.getIssueIds() != null && !request.getIssueIds().isEmpty()) {
            return issueRepository.findAllById(request.getIssueIds());
        }
        return issueRepository.findAll().stream()
                .filter(issue -> isBlank(request.getCluster()) || request.getCluster().equalsIgnoreCase(firstNonBlank(issue.getClusterCode(), issue.getClusterName())))
                .filter(issue -> isBlank(request.getStatus()) || request.getStatus().equalsIgnoreCase(firstNonBlank(issue.getStatus(), "")))
                .filter(issue -> request.getIssueTypes() == null || request.getIssueTypes().isEmpty() || request.getIssueTypes().contains(issue.getIssueType()))
                .limit(100)
                .collect(Collectors.toList());
    }

    private Map<String, Object> issueContext(AccessGovernanceIssue issue) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", issue.getId());
        context.put("cluster", firstNonBlank(issue.getClusterCode(), issue.getClusterName()));
        context.put("authBackend", issue.getSourceSystems());
        context.put("issueType", issue.getIssueType());
        context.put("issueTypeLabel", riskTypeLabel(issue.getIssueType()));
        context.put("severity", issue.getSeverity());
        context.put("status", issue.getStatus());
        context.put("principalType", issue.getResourceType());
        context.put("principalName", issue.getUsername());
        context.put("databaseName", issue.getDatabaseName());
        context.put("tableName", issue.getTableName());
        context.put("permission", issue.getPermission());
        context.put("owner", issue.getOwner());
        context.put("reason", firstNonBlank(issue.getEvidence(), issue.getRecommendation()));
        context.put("lastUsedAt", issue.getLastActiveAt() == null ? null : issue.getLastActiveAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        context.put("detectedAt", issue.getDetectedAt() == null ? null : issue.getDetectedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return context;
    }

    private Map<String, Object> fallback(String mode, List<Map<String, Object>> contexts) {
        if ("REPORT".equals(mode)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("markdown", fallbackReportFromContexts(contexts));
            return result;
        }
        if ("PLAN".equals(mode)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", "已根据当前风险类型和等级生成治理计划，需管理员确认后执行。");
            result.put("actions", fallbackActionsFromContexts(contexts));
            return result;
        }
        Map<String, Object> first = contexts.isEmpty() ? new LinkedHashMap<>() : contexts.get(0);
        Map<String, Object> advice = new LinkedHashMap<>();
        advice.put("summary", first.getOrDefault("issueTypeLabel", "权限风险") + "：" + first.getOrDefault("principalName", "未知账号"));
        advice.put("riskReason", first.getOrDefault("reason", "该风险需要管理员复核"));
        advice.put("suggestedAction", fallbackAction(String.valueOf(first.get("issueType")), String.valueOf(first.get("severity"))));
        advice.put("suggestedOwner", first.getOrDefault("owner", ""));
        advice.put("confidence", 0.62);
        advice.put("approvalRequired", true);
        advice.put("steps", Arrays.asList("核对账号、资源和权限范围", "确认负责人或审批依据", "管理员确认后执行指派、关闭或回收"));
        advice.put("evidence", Arrays.asList(String.valueOf(first.getOrDefault("issueTypeLabel", "权限风险")), String.valueOf(first.getOrDefault("permission", "-")), String.valueOf(first.getOrDefault("lastUsedAt", "-"))));
        return advice;
    }

    private List<Map<String, Object>> fallbackActions(List<AccessGovernanceIssue> issues) {
        return issues.stream().collect(Collectors.groupingBy(issue -> fallbackAction(issue))).entrySet().stream().map(entry -> {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", entry.getKey());
            action.put("issueIds", entry.getValue().stream().map(AccessGovernanceIssue::getId).collect(Collectors.toList()));
            action.put("reason", "基于风险类型和等级的规则建议");
            action.put("requiresApproval", true);
            return action;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> fallbackActionsFromContexts(List<Map<String, Object>> contexts) {
        return contexts.stream().collect(Collectors.groupingBy(ctx -> fallbackAction(String.valueOf(ctx.get("issueType")), String.valueOf(ctx.get("severity"))))).entrySet().stream().map(entry -> {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", entry.getKey());
            action.put("issueIds", entry.getValue().stream().map(ctx -> ctx.get("id")).collect(Collectors.toList()));
            action.put("reason", "基于风险类型和等级的规则建议");
            action.put("requiresApproval", true);
            return action;
        }).collect(Collectors.toList());
    }

    private String fallbackPlanSummary(List<AccessGovernanceIssue> issues) {
        return "共识别 " + issues.size() + " 条风险，建议优先处理高权限、无人负责和长期未使用权限。";
    }

    private String fallbackReport(List<AccessGovernanceIssue> issues) {
        return fallbackReportFromContexts(issues.stream().map(this::issueContext).collect(Collectors.toList()));
    }

    private String fallbackReportFromContexts(List<Map<String, Object>> contexts) {
        Map<String, Long> typeCounts = contexts.stream().collect(Collectors.groupingBy(ctx -> String.valueOf(ctx.getOrDefault("issueTypeLabel", ctx.get("issueType"))), LinkedHashMap::new, Collectors.counting()));
        StringBuilder sb = new StringBuilder();
        sb.append("## 权限风险治理报告\n\n");
        sb.append("### 风险总览\n\n");
        sb.append("- 风险总数：").append(contexts.size()).append(" 条\n");
        typeCounts.forEach((type, count) -> sb.append("- ").append(type).append("：").append(count).append(" 条\n"));
        sb.append("\n### 治理建议\n\n");
        sb.append("1. 优先处理高权限、无人负责和长期未使用权限。\n");
        sb.append("2. 对可回收风险先生成计划，再由管理员确认执行。\n");
        sb.append("3. 对公共账号或生产账号先指派负责人，再进入复核闭环。\n");
        return sb.toString();
    }

    private List<String> evidence(AccessGovernanceIssue issue) {
        List<String> evidence = new ArrayList<>();
        evidence.add("风险类型：" + riskTypeLabel(issue.getIssueType()));
        evidence.add("权限：" + firstNonBlank(issue.getPermission(), "-"));
        evidence.add("最近使用：" + (issue.getLastActiveAt() == null ? "-" : issue.getLastActiveAt()));
        return evidence;
    }

    private String fallbackAction(AccessGovernanceIssue issue) {
        return fallbackAction(issue.getIssueType(), issue.getSeverity());
    }

    private String fallbackAction(String issueType, String severity) {
        String type = firstNonBlank(issueType, "").toUpperCase(Locale.ROOT);
        if (type.contains("UNOWNED")) return "ASSIGN_OWNER";
        if (type.contains("UNUSED") || type.contains("STALE")) return "REVOKE";
        if (type.contains("HIGH") || "HIGH".equalsIgnoreCase(severity)) return "REVIEW_MANUALLY";
        if (type.contains("EXCESS")) return "DOWNGRADE";
        return "REVIEW_MANUALLY";
    }

    private String riskTypeLabel(String issueType) {
        if (issueType == null) return "权限风险";
        switch (issueType) {
            case "OPEN_LOOP": return "未闭环风险";
            case "UNUSED_PERMISSION": return "未使用权限";
            case "EXCESS_ROLE": return "超出角色";
            case "HIGH_PRIVILEGE_REVIEW": return "高权限复核";
            case "UNOWNED_PERMISSION": return "无人负责权限";
            case "STALE_EMPTY_GROUP": return "长期空组";
            default: return issueType;
        }
    }

    private String inferCluster(List<AccessGovernanceIssue> issues) {
        return issues.stream().map(issue -> firstNonBlank(issue.getClusterCode(), issue.getClusterName())).filter(value -> !isBlank(value)).findFirst().orElse("");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Double number(Object value, double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value;
        }
        return "";
    }
}
