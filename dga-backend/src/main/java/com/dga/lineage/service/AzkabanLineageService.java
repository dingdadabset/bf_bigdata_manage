package com.dga.lineage.service;

import com.dga.lineage.entity.DataLineage;
import com.dga.lineage.repository.DataLineageRepository;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.TableMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class AzkabanLineageService {

    private static final Logger log = LoggerFactory.getLogger(AzkabanLineageService.class);

    @Autowired
    private DataLineageRepository lineageRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    // Pattern to match 'db.table' format
    // Matches: FROM db.table, JOIN db.table, INTO TABLE db.table, etc.
    // Enhanced regex to avoid matching things that look like tables but aren't (e.g. inside strings)
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\b([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\b");
    
    // Pattern to detect potential SQL commands in job files
    private static final Pattern SQL_COMMAND_PATTERN = Pattern.compile("(select|insert|create|drop|alter)\\s+", Pattern.CASE_INSENSITIVE);

    /**
     * Parse lineage from Azkaban database
     */
    public void parseAzkabanLineage(String host, int port, String dbName, String user, String password) {
        parseAzkabanLineage(host, port, dbName, user, password, null, null);
    }

    public void parseAzkabanLineage(String host, int port, String dbName, String user, String password,
                                    String clusterCode, Long dataSourceId) {
        log.info("Starting Azkaban lineage parsing from {}:{}/{}", host, port, dbName);
        
        try {
            DataSource ds = DataSourceBuilder.create()
                    .url("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true")
                    .username(user)
                    .password(password)
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .build();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

            // 1. Get all active projects
            String projectsSql = "SELECT id, name, version FROM projects WHERE active = 1";
            List<Map<String, Object>> projects = jdbcTemplate.queryForList(projectsSql);

            log.info("Found {} active projects", projects.size());

            for (Map<String, Object> project : projects) {
                Integer projectId = (Integer) project.get("id");
                String projectName = (String) project.get("name");
                Integer version = (Integer) project.get("version");

                if (version == null) continue;

                try {
                    // 2. Get the file resource_id for this version
                    // Note: Schema might vary. Standard Azkaban has project_versions table.
                    // If table doesn't exist, this will throw, which is fine (caught below).
                    
                    // Handle potential chunking in project_files
                    String fileSql = "SELECT file FROM project_files WHERE project_id = ? AND version = ? ORDER BY chunk";
                    
                    // Fetch the BLOBs
                    List<byte[]> chunks = null;
                    try {
                        chunks = jdbcTemplate.query(fileSql, new Object[]{projectId, version}, (rs, rowNum) -> rs.getBytes("file"));
                    } catch (Exception e) {
                        // Try alternative schema (no chunking)
                         try {
                            String simpleFileSql = "SELECT file FROM project_files WHERE project_id = ? AND version = ?";
                            byte[] singleChunk = jdbcTemplate.queryForObject(simpleFileSql, new Object[]{projectId, version}, byte[].class);
                            if (singleChunk != null) {
                                chunks = Collections.singletonList(singleChunk);
                            }
                        } catch (Exception ex) {
                             log.warn("Could not fetch project file for {}: {}", projectName, ex.getMessage());
                             continue;
                        }
                    }
                    
                    if (chunks != null && !chunks.isEmpty()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            for (byte[] chunk : chunks) {
                                if (chunk != null) baos.write(chunk);
                            }
                            parseProjectZip(projectName, baos.toByteArray(), clusterCode, dataSourceId);
                        } catch (IOException e) {
                            log.error("Error assembling project chunks for {}: {}", projectName, e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse project {}: {}", projectName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to connect to Azkaban DB: {}", e.getMessage(), e);
            throw new RuntimeException("Azkaban connection failed", e);
        }
    }

    private void parseProjectZip(String projectName, byte[] zipBytes, String clusterCode, Long dataSourceId) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && (entry.getName().endsWith(".job") || entry.getName().endsWith(".flow"))) {
                    String content = readZipEntry(zis);
                    parseJobContent(projectName, entry.getName(), content, clusterCode, dataSourceId);
                }
            }
        } catch (IOException e) {
            log.error("Error unzipping project file for {}", projectName, e);
        }
    }

    private String readZipEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = zis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
    
    private void parseJobContent(String projectName, String fileName, String content, String clusterCode, Long dataSourceId) {
        Properties props = new Properties();
        try {
            props.load(new java.io.StringReader(content));
        } catch (IOException e) {
            return;
        }

        String type = props.getProperty("type");
        // Only interested in Hive or Command jobs that execute SQL
        if ("command".equals(type) || "hive".equals(type) || "spark".equals(type)) {
            String command = props.getProperty("command");
            String query = props.getProperty("query"); // Some custom job types might use 'query'
            
            // If command is null, check if it's a hive script execution
            // e.g. hive -f script.q
            
            String sqlToParse = null;
            if (command != null && SQL_COMMAND_PATTERN.matcher(command).find()) {
                sqlToParse = command;
            } else if (query != null) {
                sqlToParse = query;
            } else if (command != null && command.contains("hive -e")) {
                // Extract the query from hive -e "..."
                // Simple heuristic
                int start = command.indexOf("\"");
                int end = command.lastIndexOf("\"");
                if (start != -1 && end > start) {
                    sqlToParse = command.substring(start + 1, end);
                }
            }

            if (sqlToParse != null) {
                extractTableLineage(projectName, fileName, sqlToParse, clusterCode, dataSourceId);
            }
        }
    }

    private void extractTableLineage(String projectName, String jobName, String command, String clusterCode, Long dataSourceId) {
        // Find all potential tables
        Matcher matcher = TABLE_PATTERN.matcher(command);
        Set<String> potentialTables = new HashSet<>();
        
        while (matcher.find()) {
            potentialTables.add(matcher.group(0)); // db.table
        }

        // Identify target table (simple heuristic: INSERT INTO table)
        // This is very rough. A real SQL parser is needed for accuracy.
        String targetTable = null;
        Pattern insertPattern = Pattern.compile("INSERT\\s+(?:INTO\\s+|OVERWRITE\\s+)(?:TABLE\\s+)?([a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(command);
        if (insertMatcher.find()) {
            targetTable = insertMatcher.group(1);
        }

        // Identify source tables (everything else)
        Set<String> sourceTables = new HashSet<>();
        for (String tbl : potentialTables) {
            if (!tbl.equalsIgnoreCase(targetTable)) {
                sourceTables.add(tbl);
            }
        }

        if (targetTable != null && !sourceTables.isEmpty()) {
            saveLineage(projectName + ":" + jobName, sourceTables, targetTable, command, clusterCode, dataSourceId);
        }
    }

    private void saveLineage(String jobIdentifier, Set<String> sources, String target, String command,
                             String clusterCode, Long dataSourceId) {
        TableMetadata targetMeta = findTableMetadata(target, clusterCode, dataSourceId);
        if (targetMeta == null) {
            log.debug("Target table metadata not found: {}", target);
            return;
        }

        for (String source : sources) {
            TableMetadata sourceMeta = findTableMetadata(source, clusterCode, dataSourceId);
            if (sourceMeta != null) {
                // Check if exists to avoid duplicates
                Optional<DataLineage> existing = lineageRepository.findBySourceTableIdAndTargetTableId(sourceMeta.getId(), targetMeta.getId());
                if (!existing.isPresent()) {
                    DataLineage lineage = new DataLineage();
                    lineage.setSourceTableId(sourceMeta.getId());
                    lineage.setTargetTableId(targetMeta.getId());
                    lineage.setLineageType("ETL");
                    lineage.setTransformationLogic("Job: " + jobIdentifier + "\nCommand: " + command);
                    lineage.setSourceType("AZKABAN_DB");
                    lineage.setDataSourceId(dataSourceId);
                    lineage.setClusterCode(clusterCode);
                    lineage.setSourceProject(jobIdentifier.contains(":") ? jobIdentifier.substring(0, jobIdentifier.indexOf(":")) : jobIdentifier);
                    lineage.setSourceTask(jobIdentifier);
                    lineage.setSourceTaskKey(jobIdentifier);
                    lineage.setSourceSqlHash(String.valueOf(command.hashCode()));
                    lineage.setStatus("ACTIVE");
                    lineage.setParsedAt(java.time.LocalDateTime.now());
                    lineageRepository.save(lineage);
                    log.info("Created lineage: {} -> {}", source, target);
                }
            }
        }
    }

    private TableMetadata findTableMetadata(String fullTableName, String clusterCode, Long dataSourceId) {
        String[] parts = fullTableName.split("\\.");
        if (parts.length != 2) return null;
        String db = parts[0];
        String tbl = parts[1];

        List<TableMetadata> tables;
        if (dataSourceId != null) {
            tables = tableMetadataRepository.findByDataSourceIdAndDbNameAndTableName(dataSourceId, db, tbl);
        } else if (StringUtils.hasText(clusterCode)) {
            tables = tableMetadataRepository.findByClusterCodeAndDbNameAndTableName(clusterCode.trim(), db, tbl);
        } else {
            tables = tableMetadataRepository.findByDbNameAndTableName(db, tbl);
        }
        if (tables != null && !tables.isEmpty()) {
            if (tables.size() == 1) {
                return tables.get(0);
            }
            log.warn("Ambiguous table metadata for {}. Provide clusterCode or dataSourceId to avoid cross-cluster lineage.", fullTableName);
        }
        return null;
    }
}
