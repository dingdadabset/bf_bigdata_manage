package com.dga.lineage.service;

import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.datasource.entity.DataSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AzkabanWebLineageCollector implements SchedulerLineageCollector {
    private static final Pattern SQL_COMMAND_PATTERN = Pattern.compile("(select|insert|create|drop|alter)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARG_PATTERN_TEMPLATE = Pattern.compile("--%s\\s+(\"([^\"]*)\"|'([^']*)'|\\S+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "create\\s+(?:external\\s+)?table\\s+(?:if\\s+not\\s+exists\\s+)?`?([a-zA-Z0-9_]+)`?\\s*\\.\\s*`?([a-zA-Z0-9_]+)`?\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLUMN_COMMENT_PATTERN = Pattern.compile(
            "^`?([a-zA-Z0-9_]+)`?\\s+([^,\\s]+(?:\\s*<[^>]+>)?)(?:\\s+COMMENT\\s+'([^']*)')?.*$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private LineageSqlParser sqlParser;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String endpointType) {
        return ClusterEndpoint.TYPE_AZKABAN_WEB.equals(endpointType);
    }

    @Override
    public LineageCollectResult collect(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId) {
        LineageCollectResult result = new LineageCollectResult();
        List<Map<String, Object>> projects = loadProjects(endpoint);
        for (Map<String, Object> project : projects) {
            String projectName = asString(project.get("name"));
            if (projectName == null) {
                continue;
            }
            try {
                collectProject(endpoint, result, projectName);
            } catch (Exception e) {
                result.addFailure(projectName + ": " + e.getMessage());
            }
        }
        return result;
    }

    private void collectProject(ClusterEndpoint endpoint, LineageCollectResult result, String projectName) throws Exception {
        JsonNode flowsPayload = getJson(endpoint, "/manager", params("ajax", "fetchprojectflows", "project", projectName));
        JsonNode flows = flowsPayload.path("flows");
        if (!flows.isArray()) {
            return;
        }
        Set<String> visited = new HashSet<>();
        for (JsonNode flow : flows) {
            String flowId = text(flow.get("flowId"));
            if (flowId != null) {
                collectFlow(endpoint, result, projectName, flowId, visited);
            }
        }
    }

    private void collectFlow(ClusterEndpoint endpoint, LineageCollectResult result, String projectName,
                             String flowName, Set<String> visited) throws Exception {
        String visitKey = projectName + "/" + flowName;
        if (!visited.add(visitKey)) {
            return;
        }
        JsonNode graph = getJson(endpoint, "/manager", params("ajax", "fetchflowgraph", "project", projectName, "flow", flowName));
        JsonNode nodes = graph.path("nodes");
        if (nodes.isArray()) {
            collectNodes(endpoint, result, projectName, flowName, nodes, visited);
        }
    }

    private void collectNodes(ClusterEndpoint endpoint, LineageCollectResult result, String projectName,
                              String flowName, JsonNode nodes, Set<String> visited) throws Exception {
        for (JsonNode node : nodes) {
            String type = text(node.get("type"));
            String id = text(node.get("id"));
            if (id == null) {
                continue;
            }
            if ("flow".equalsIgnoreCase(type)) {
                JsonNode childNodes = node.path("nodes");
                String childFlow = firstNonBlank(text(node.get("flowId")), text(node.get("flow")), id);
                if (childNodes.isArray()) {
                    collectNodes(endpoint, result, projectName, childFlow, childNodes, visited);
                } else {
                    collectFlow(endpoint, result, projectName, childFlow, visited);
                }
                continue;
            }
            if (!"command".equalsIgnoreCase(type) && !"hive".equalsIgnoreCase(type) && !"spark".equalsIgnoreCase(type)) {
                continue;
            }
            collectJob(endpoint, result, projectName, flowName, id);
        }
    }

    private void collectJob(ClusterEndpoint endpoint, LineageCollectResult result,
                            String projectName, String flowName, String jobName) {
        try {
            JsonNode job = getJson(endpoint, "/manager", params("ajax", "fetchJobInfo",
                    "project", projectName, "flowName", flowName, "jobName", jobName));
            JsonNode params = job.path("overrideParams").isObject() ? job.path("overrideParams") : job.path("generalParams");
            String command = firstNonBlank(text(params.get("command")), text(params.get("query")));
            if (command == null) {
                return;
            }
            JobContext parsed = parseJobContext(projectName, flowName, jobName, command);
            String taskKey = projectName + ":" + jobName;
            if (parsed.sql != null) {
                result.addEdges(sqlParser.parse(projectName, flowName, jobName, taskKey, parsed.sql));
                result.addContext(sqlParser.parseContext(projectName, flowName, jobName, taskKey, jobName, command, parsed.sql));
            }
            for (ParsedMetadataContextSuggestion suggestion : parsed.suggestions) {
                result.addSuggestion(suggestion);
            }
        } catch (Exception e) {
            result.addFailure(projectName + "/" + flowName + "/" + jobName + ": " + e.getMessage());
        }
    }

    private JobContext parseJobContext(String projectName, String flowName, String jobName, String command) {
        JobContext context = new JobContext();
        String schemaName = arg(command, "hive_schema_name");
        String tableName = arg(command, "hive_table_name");
        String ddl = arg(command, "hive_ddlsql");
        String tmpDdl = arg(command, "hive_ddltmpsql");
        String sql = firstNonBlank(ddl, command);
        context.sql = SQL_COMMAND_PATTERN.matcher(sql).find() ? sql : null;

        if (schemaName != null && tableName != null) {
            ParsedMetadataContextSuggestion tableSuggestion = baseSuggestion(projectName, flowName, jobName);
            tableSuggestion.setDbName(normalize(schemaName));
            tableSuggestion.setTableName(normalize(tableName));
            tableSuggestion.setContextType("SCHEDULER_CONTEXT");
            tableSuggestion.setSuggestedValue("Azkaban 项目 " + projectName + " / Flow " + flowName
                    + " / Job " + jobName + " 维护或生成该表");
            tableSuggestion.setEvidence(trim(command, 1200));
            tableSuggestion.setConfidence(ddl == null ? "MEDIUM" : "HIGH");
            context.suggestions.add(tableSuggestion);
        }

        parseDdlColumns(context.suggestions, projectName, flowName, jobName, firstNonBlank(ddl, tmpDdl), schemaName, tableName);
        return context;
    }

    private void parseDdlColumns(List<ParsedMetadataContextSuggestion> suggestions, String projectName,
                                 String flowName, String jobName, String ddl, String fallbackDb, String fallbackTable) {
        if (ddl == null) {
            return;
        }
        Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(ddl);
        String dbName = normalize(fallbackDb);
        String tableName = normalize(fallbackTable);
        int columnStart = -1;
        if (tableMatcher.find()) {
            dbName = normalize(tableMatcher.group(1));
            tableName = normalize(tableMatcher.group(2));
            columnStart = tableMatcher.end();
        }
        if (dbName == null || tableName == null) {
            return;
        }
        if (columnStart < 0) {
            int open = ddl.indexOf('(');
            columnStart = open < 0 ? -1 : open + 1;
        }
        int columnEnd = matchingParenEnd(ddl, columnStart - 1);
        if (columnStart < 0 || columnEnd <= columnStart) {
            return;
        }
        for (String rawColumn : splitTopLevel(ddl.substring(columnStart, columnEnd))) {
            String line = rawColumn.trim();
            if (line.isEmpty() || line.toLowerCase(Locale.ROOT).startsWith("partitioned")) {
                continue;
            }
            Matcher columnMatcher = COLUMN_COMMENT_PATTERN.matcher(line);
            if (!columnMatcher.matches()) {
                continue;
            }
            String columnName = normalize(columnMatcher.group(1));
            String comment = clean(columnMatcher.group(3));
            if (comment == null) {
                continue;
            }
            ParsedMetadataContextSuggestion suggestion = baseSuggestion(projectName, flowName, jobName);
            suggestion.setDbName(dbName);
            suggestion.setTableName(tableName);
            suggestion.setColumnName(columnName);
            suggestion.setContextType("COLUMN_COMMENT");
            suggestion.setSuggestedValue(comment);
            suggestion.setEvidence(trim(line, 1000));
            suggestion.setConfidence("HIGH");
            suggestions.add(suggestion);
        }
    }

    private List<Map<String, Object>> loadProjects(ClusterEndpoint webEndpoint) {
        ClusterEndpoint dbEndpoint = endpointRepository
                .findByClusterCodeAndEndpointTypeAndStatus(webEndpoint.getClusterCode(), ClusterEndpoint.TYPE_AZKABAN_DB, "ACTIVE")
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("AZKABAN_WEB 需要同集群配置 AZKABAN_DB 端点用于读取项目清单"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource(dbEndpoint));
        return jdbcTemplate.queryForList("SELECT id, name, version FROM projects WHERE active = 1");
    }

    private JsonNode getJson(ClusterEndpoint endpoint, String path, Map<String, String> params) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(endpoint.getUrl()) + path);
        params.forEach(builder::queryParam);
        URI uri = builder.build().encode().toUri();
        String body = restTemplate.getForObject(uri, String.class);
        return objectMapper.readTree(body == null ? "{}" : body);
    }

    private Map<String, String> params(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }

    private DataSource mysqlDataSource(ClusterEndpoint endpoint) {
        return DataSourceBuilder.create()
                .url(endpoint.getUrl())
                .username(endpoint.getUsername())
                .password(endpoint.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    private ParsedMetadataContextSuggestion baseSuggestion(String projectName, String flowName, String jobName) {
        ParsedMetadataContextSuggestion suggestion = new ParsedMetadataContextSuggestion();
        suggestion.setProjectName(projectName);
        suggestion.setFlowName(flowName);
        suggestion.setJobName(jobName);
        return suggestion;
    }

    private String arg(String command, String name) {
        Pattern pattern = Pattern.compile(String.format(ARG_PATTERN_TEMPLATE.pattern(), Pattern.quote(name)),
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(command == null ? "" : command);
        if (!matcher.find()) {
            return null;
        }
        return clean(firstNonBlank(matcher.group(2), matcher.group(3), stripQuotes(matcher.group(1))));
    }

    private int matchingParenEnd(String text, int openIndex) {
        if (text == null || openIndex < 0 || openIndex >= text.length()) {
            return -1;
        }
        int depth = 0;
        boolean inQuote = false;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (inQuote) {
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private List<String> splitTopLevel(String text) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuote = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (!inQuote) {
                if (c == '(' || c == '<') depth++;
                if ((c == ')' || c == '>') && depth > 0) depth--;
            }
            if (c == ',' && depth == 0 && !inQuote) {
                items.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            items.add(current.toString());
        }
        return items;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String clean(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.replace("`", "").trim();
    }

    private String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimTrailingSlash(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalStateException("Azkaban Web URL 不能为空");
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    private static class JobContext {
        private String sql;
        private final List<ParsedMetadataContextSuggestion> suggestions = new ArrayList<>();
    }
}
