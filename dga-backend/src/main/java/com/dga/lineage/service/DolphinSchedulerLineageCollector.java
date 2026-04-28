package com.dga.lineage.service;

import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.datasource.entity.DataSourceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DolphinSchedulerLineageCollector implements SchedulerLineageCollector {

    private static final Pattern SQL_PATTERN = Pattern.compile("(select|insert|create|drop|alter)\\s+", Pattern.CASE_INSENSITIVE);

    @Autowired
    private LineageSqlParser sqlParser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String endpointType) {
        return ClusterEndpoint.TYPE_DOLPHINSCHEDULER_DB.equals(endpointType);
    }

    @Override
    public LineageCollectResult collect(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId) {
        LineageCollectResult result = new LineageCollectResult();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource(endpoint));
        List<Map<String, Object>> tasks = queryTaskDefinitions(jdbcTemplate);
        for (Map<String, Object> task : tasks) {
            String projectName = firstNonBlank(asString(task.get("project_name")), asString(task.get("project_code")));
            String workflowName = asString(task.get("workflow_name"));
            String taskName = asString(task.get("task_name"));
            String taskKey = firstNonBlank(asString(task.get("task_code")), projectName + ":" + taskName);
            String taskParams = asString(task.get("task_params"));
            try {
                for (String sql : extractSqlValues(taskParams)) {
                    result.addEdges(sqlParser.parse(projectName, workflowName, taskName, taskKey, sql));
                }
            } catch (Exception e) {
                result.addFailure(projectName + "/" + workflowName + "/" + taskName + ": " + e.getMessage());
            }
        }
        return result;
    }

    private List<Map<String, Object>> queryTaskDefinitions(JdbcTemplate jdbcTemplate) {
        String richSql = "SELECT p.name AS project_name, td.project_code AS project_code, " +
                "pd.name AS workflow_name, td.code AS task_code, td.name AS task_name, td.task_type, td.task_params " +
                "FROM t_ds_task_definition td " +
                "LEFT JOIN t_ds_project p ON p.code = td.project_code " +
                "LEFT JOIN t_ds_process_task_relation r ON r.post_task_code = td.code AND r.project_code = td.project_code " +
                "LEFT JOIN t_ds_process_definition pd ON pd.code = r.process_definition_code AND pd.project_code = td.project_code " +
                "WHERE td.task_type IN ('SQL','SHELL','SPARK','FLINK','PROCEDURE')";
        try {
            return jdbcTemplate.queryForList(richSql);
        } catch (Exception e) {
            String fallbackSql = "SELECT td.project_code AS project_code, td.code AS task_code, td.name AS task_name, " +
                    "td.task_type, td.task_params FROM t_ds_task_definition td " +
                    "WHERE td.task_type IN ('SQL','SHELL','SPARK','FLINK','PROCEDURE')";
            return jdbcTemplate.queryForList(fallbackSql);
        }
    }

    private List<String> extractSqlValues(String taskParams) throws Exception {
        List<String> values = new ArrayList<>();
        if (taskParams == null || taskParams.trim().isEmpty()) {
            return values;
        }
        JsonNode root = objectMapper.readTree(taskParams);
        collectSqlValues(root, "", values);
        return values;
    }

    private void collectSqlValues(JsonNode node, String key, List<String> values) {
        if (node == null) {
            return;
        }
        if (node.isTextual()) {
            String text = node.asText();
            String lowerKey = key == null ? "" : key.toLowerCase();
            if ((lowerKey.contains("sql") || lowerKey.contains("script") || lowerKey.contains("command"))
                    && SQL_PATTERN.matcher(text).find()) {
                values.add(text);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectSqlValues(child, key, values);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                collectSqlValues(field.getValue(), field.getKey(), values);
            }
        }
    }

    private DataSource mysqlDataSource(ClusterEndpoint endpoint) {
        return DataSourceBuilder.create()
                .url(endpoint.getUrl())
                .username(endpoint.getUsername())
                .password(endpoint.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
