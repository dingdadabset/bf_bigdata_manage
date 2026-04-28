package com.dga.lineage.service;

import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.datasource.entity.DataSourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AzkabanSchedulerLineageCollector implements SchedulerLineageCollector {

    private static final Pattern SQL_COMMAND_PATTERN = Pattern.compile("(select|insert|create|drop|alter)\\s+", Pattern.CASE_INSENSITIVE);

    @Autowired
    private LineageSqlParser sqlParser;

    @Override
    public boolean supports(String endpointType) {
        return ClusterEndpoint.TYPE_AZKABAN_DB.equals(endpointType);
    }

    @Override
    public LineageCollectResult collect(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId) {
        LineageCollectResult result = new LineageCollectResult();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource(endpoint));
        List<Map<String, Object>> projects = jdbcTemplate.queryForList("SELECT id, name, version FROM projects WHERE active = 1");

        for (Map<String, Object> project : projects) {
            Integer projectId = asInteger(project.get("id"));
            Integer version = asInteger(project.get("version"));
            String projectName = asString(project.get("name"));
            if (projectId == null || version == null) {
                continue;
            }
            try {
                List<byte[]> chunks = readProjectFileChunks(jdbcTemplate, projectId, version);
                if (chunks == null || chunks.isEmpty()) {
                    continue;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] chunk : chunks) {
                    if (chunk != null) {
                        output.write(chunk);
                    }
                }
                result.addEdges(parseProjectZip(projectName, output.toByteArray()));
            } catch (Exception e) {
                result.addFailure(projectName + ": " + e.getMessage());
            }
        }
        return result;
    }

    private List<byte[]> readProjectFileChunks(JdbcTemplate jdbcTemplate, Integer projectId, Integer version) {
        try {
            return jdbcTemplate.query("SELECT file FROM project_files WHERE project_id = ? AND version = ? ORDER BY chunk",
                    new Object[]{projectId, version}, (rs, rowNum) -> rs.getBytes("file"));
        } catch (Exception e) {
            try {
                byte[] single = jdbcTemplate.queryForObject(
                        "SELECT file FROM project_files WHERE project_id = ? AND version = ?",
                        new Object[]{projectId, version}, byte[].class);
                return single == null ? Collections.emptyList() : Collections.singletonList(single);
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        }
    }

    private List<ParsedLineageEdge> parseProjectZip(String projectName, byte[] zipBytes) throws IOException {
        LineageCollectResult projectResult = new LineageCollectResult();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || (!entry.getName().endsWith(".job") && !entry.getName().endsWith(".flow"))) {
                    continue;
                }
                String content = readZipEntry(zis);
                String sql = extractSqlFromAzkabanJob(content);
                if (sql != null) {
                    projectResult.addEdges(sqlParser.parse(projectName, null, entry.getName(), projectName + ":" + entry.getName(), sql));
                }
            }
        }
        return projectResult.getEdges();
    }

    private String extractSqlFromAzkabanJob(String content) {
        Properties props = new Properties();
        try {
            props.load(new java.io.StringReader(content));
        } catch (IOException e) {
            return null;
        }
        String type = props.getProperty("type");
        if (!"command".equals(type) && !"hive".equals(type) && !"spark".equals(type)) {
            return null;
        }

        String command = props.getProperty("command");
        String query = props.getProperty("query");
        if (query != null && SQL_COMMAND_PATTERN.matcher(query).find()) {
            return query;
        }
        if (command == null) {
            return null;
        }
        if (SQL_COMMAND_PATTERN.matcher(command).find()) {
            return command;
        }
        if (command.contains("hive -e")) {
            int start = command.indexOf("\"");
            int end = command.lastIndexOf("\"");
            if (start != -1 && end > start) {
                return command.substring(start + 1, end);
            }
        }
        return null;
    }

    private String readZipEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = zis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private DataSource mysqlDataSource(ClusterEndpoint endpoint) {
        return DataSourceBuilder.create()
                .url(endpoint.getUrl())
                .username(endpoint.getUsername())
                .password(endpoint.getPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
