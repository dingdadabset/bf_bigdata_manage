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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        String projectDir = configuredProjectDir(endpoint);

        for (Map<String, Object> project : projects) {
            Integer projectId = asInteger(project.get("id"));
            Integer version = asInteger(project.get("version"));
            String projectName = asString(project.get("name"));
            if (projectId == null || version == null) {
                continue;
            }
            try {
                if (projectDir != null) {
                    LineageCollectResult projectResult = parseProjectDirectory(projectName, projectId, version, projectDir);
                    result.addEdges(projectResult.getEdges());
                    result.addContexts(projectResult.getContexts());
                    projectResult.getFailures().forEach(result::addFailure);
                    continue;
                }
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
                LineageCollectResult projectResult = parseProjectZip(projectName, output.toByteArray());
                result.addEdges(projectResult.getEdges());
                result.addContexts(projectResult.getContexts());
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

    private LineageCollectResult parseProjectDirectory(String projectName, Integer projectId, Integer version, String projectDir) throws IOException {
        LineageCollectResult projectResult = new LineageCollectResult();
        Path projectPath = Paths.get(projectDir, projectId + "." + version);
        if (!Files.isDirectory(projectPath)) {
            projectResult.addFailure(projectName + ": 项目目录不存在 " + projectPath);
            return projectResult;
        }
        List<Path> jobFiles;
        try (Stream<Path> stream = Files.walk(projectPath, 3)) {
            jobFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".job")
                            || path.getFileName().toString().endsWith(".flow"))
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toList());
        }
        for (Path jobFile : jobFiles) {
            String content = new String(Files.readAllBytes(jobFile), StandardCharsets.UTF_8);
            parseJobContent(projectResult, projectName, projectPath.relativize(jobFile).toString(), content);
        }
        return projectResult;
    }

    private LineageCollectResult parseProjectZip(String projectName, byte[] zipBytes) throws IOException {
        LineageCollectResult projectResult = new LineageCollectResult();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || (!entry.getName().endsWith(".job") && !entry.getName().endsWith(".flow"))) {
                    continue;
                }
                String content = readZipEntry(zis);
                parseJobContent(projectResult, projectName, entry.getName(), content);
            }
        }
        return projectResult;
    }

    private void parseJobContent(LineageCollectResult projectResult, String projectName, String jobPath, String content) {
        AzkabanJobSql jobSql = extractSqlFromAzkabanJob(content);
        if (jobSql == null) {
            return;
        }
        String taskName = stripExtension(Paths.get(jobPath).getFileName().toString());
        String workflowName = inferWorkflowName(taskName);
        String taskKey = projectName + ":" + taskName;
        projectResult.addEdges(sqlParser.parse(projectName, workflowName, taskName, taskKey, jobSql.sql));
        projectResult.addContext(sqlParser.parseContext(projectName, workflowName, taskName, taskKey, jobPath, jobSql.commandText, jobSql.sql));
    }

    private AzkabanJobSql extractSqlFromAzkabanJob(String content) {
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
            return new AzkabanJobSql(query, query);
        }
        if (command == null) {
            return null;
        }
        if (SQL_COMMAND_PATTERN.matcher(command).find()) {
            return new AzkabanJobSql(command, command);
        }
        if (command.contains("hive -e")) {
            int start = command.indexOf("\"");
            int end = command.lastIndexOf("\"");
            if (start != -1 && end > start) {
                return new AzkabanJobSql(command.substring(start + 1, end), command);
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

    private String configuredProjectDir(ClusterEndpoint endpoint) {
        List<String> candidates = new ArrayList<>();
        candidates.add(endpoint.getDescription());
        candidates.add(endpoint.getServiceName());
        candidates.add(endpoint.getBaseDn());
        for (String candidate : candidates) {
            String value = readKeyValue(candidate, "projectDir");
            if (value == null) {
                value = readKeyValue(candidate, "azkaban.project.dir");
            }
            if (value != null && Files.isDirectory(Paths.get(value))) {
                return value;
            }
        }
        return null;
    }

    private String readKeyValue(String text, String key) {
        if (text == null || key == null) {
            return null;
        }
        for (String part : text.split("[;\\n,]")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(key + "=")) {
                String value = trimmed.substring((key + "=").length()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private String stripExtension(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String inferWorkflowName(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            return null;
        }
        if ("start".equalsIgnoreCase(taskName) || "end".equalsIgnoreCase(taskName)) {
            return taskName;
        }
        int seqIndex = taskName.lastIndexOf("_seq");
        if (seqIndex > 0) {
            return taskName.substring(0, seqIndex);
        }
        return taskName;
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

    private static class AzkabanJobSql {
        private final String sql;
        private final String commandText;

        private AzkabanJobSql(String sql, String commandText) {
            this.sql = sql;
            this.commandText = commandText;
        }
    }
}
