package com.dga.lineage.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
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
