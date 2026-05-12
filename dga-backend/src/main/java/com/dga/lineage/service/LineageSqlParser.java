package com.dga.lineage.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LineageSqlParser {

    private static final Pattern TABLE_PATTERN = Pattern.compile("\\b`?([a-zA-Z0-9_]+)`?\\.`?([a-zA-Z0-9_]+)`?\\b");
    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "\\binsert\\s+(?:into\\s+|overwrite\\s+)(?:table\\s+)?`?([a-zA-Z0-9_]+)`?\\.`?([a-zA-Z0-9_]+)`?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_PATTERN = Pattern.compile(
            "\\b(?:from|join)\\s+`?([a-zA-Z0-9_]+)`?\\.`?([a-zA-Z0-9_]+)`?",
            Pattern.CASE_INSENSITIVE);

    public List<ParsedLineageEdge> parse(String projectName, String workflowName, String taskName, String taskKey, String sql) {
        List<ParsedLineageEdge> edges = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return edges;
        }

        Set<String> potentialTables = new HashSet<>();
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            potentialTables.add(normalize(tableMatcher.group(1)) + "." + normalize(tableMatcher.group(2)));
        }

        Set<String> targets = new HashSet<>();
        Matcher insertMatcher = INSERT_PATTERN.matcher(sql);
        while (insertMatcher.find()) {
            targets.add(normalize(insertMatcher.group(1)) + "." + normalize(insertMatcher.group(2)));
        }

        if (targets.isEmpty() || potentialTables.isEmpty()) {
            return edges;
        }

        String hash = sha256(sql);
        for (String target : targets) {
            for (String source : potentialTables) {
                if (source.equalsIgnoreCase(target)) {
                    continue;
                }
                String[] sourceParts = source.split("\\.");
                String[] targetParts = target.split("\\.");
                if (sourceParts.length != 2 || targetParts.length != 2) {
                    continue;
                }
                ParsedLineageEdge edge = new ParsedLineageEdge();
                edge.setSourceDb(sourceParts[0]);
                edge.setSourceTable(sourceParts[1]);
                edge.setTargetDb(targetParts[0]);
                edge.setTargetTable(targetParts[1]);
                edge.setSourceProject(projectName);
                edge.setSourceWorkflow(workflowName);
                edge.setSourceTask(taskName);
                edge.setSourceTaskKey(taskKey);
                edge.setSql(sql);
                edge.setSqlHash(hash);
                edges.add(edge);
            }
        }
        return edges;
    }

    public ParsedSchedulerTaskContext parseContext(String projectName, String workflowName, String taskName,
                                                   String taskKey, String jobPath, String commandText, String sql) {
        ParsedSchedulerTaskContext context = new ParsedSchedulerTaskContext();
        context.setProjectName(projectName);
        context.setWorkflowName(workflowName);
        context.setTaskName(taskName);
        context.setTaskKey(taskKey);
        context.setJobPath(jobPath);
        context.setCommandText(commandText);
        if (sql == null || sql.trim().isEmpty()) {
            context.setParseStatus("SKIPPED");
            context.setParseMessage("未识别到 SQL");
            return context;
        }

        Set<String> outputs = new LinkedHashSet<>();
        Matcher insertMatcher = INSERT_PATTERN.matcher(sql);
        while (insertMatcher.find()) {
            outputs.add(normalize(insertMatcher.group(1)) + "." + normalize(insertMatcher.group(2)));
        }

        Set<String> inputs = new LinkedHashSet<>();
        Matcher sourceMatcher = SOURCE_PATTERN.matcher(sql);
        while (sourceMatcher.find()) {
            inputs.add(normalize(sourceMatcher.group(1)) + "." + normalize(sourceMatcher.group(2)));
        }

        if (inputs.isEmpty()) {
            Set<String> potentialTables = new LinkedHashSet<>();
            Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
            while (tableMatcher.find()) {
                potentialTables.add(normalize(tableMatcher.group(1)) + "." + normalize(tableMatcher.group(2)));
            }
            for (String table : potentialTables) {
                if (!outputs.contains(table)) {
                    inputs.add(table);
                }
            }
        }

        context.setInputTables(new ArrayList<>(inputs));
        context.setOutputTables(new ArrayList<>(outputs));
        if (inputs.isEmpty() && outputs.isEmpty()) {
            context.setParseStatus("SKIPPED");
            context.setParseMessage("SQL 中未识别到 db.table");
        } else if (inputs.isEmpty() || outputs.isEmpty()) {
            context.setParseStatus("PARTIAL");
            context.setParseMessage("仅识别到输入或输出表");
        } else {
            context.setParseStatus("SUCCESS");
        }
        return context;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("`", "").trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }
}
