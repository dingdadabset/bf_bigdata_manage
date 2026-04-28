package com.dga.lineage.service;

import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.lineage.entity.DataLineage;
import com.dga.lineage.entity.LineageParseTask;
import com.dga.lineage.repository.DataLineageRepository;
import com.dga.lineage.repository.LineageParseTaskRepository;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LineageCollectionService {

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private DataLineageRepository lineageRepository;

    @Autowired
    private LineageParseTaskRepository taskRepository;

    @Autowired
    private List<SchedulerLineageCollector> collectors;

    @Transactional
    public LineageParseTask collect(Long endpointId, Long dataSourceId, String triggeredBy) {
        ClusterEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调度源端点不存在: " + endpointId));
        DataSourceConfig dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hive 数据源不存在: " + dataSourceId));
        SchedulerLineageCollector collector = collectors.stream()
                .filter(item -> item.supports(endpoint.getEndpointType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的调度源类型: " + endpoint.getEndpointType()));

        String runId = UUID.randomUUID().toString();
        LineageParseTask task = createRunningTask(endpoint, dataSource, runId, triggeredBy);
        try {
            LineageCollectResult result = collector.collect(endpoint, dataSource, runId);
            int saved = replaceActiveLineage(endpoint, dataSource, runId, result);
            task.setSuccessEdgeCount(saved);
            task.setFailedEdgeCount(result.getFailures().size());
            task.setStatus(result.getFailures().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS");
            task.setMessage("解析完成，写入血缘边 " + saved + " 条");
            task.setErrorDetail(String.join("\n", result.getFailures()));
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setMessage(e.getMessage());
            task.setErrorDetail(stackMessage(e));
        }
        task.setFinishedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    public List<LineageParseTask> latestTasks(Long sourceEndpointId, Long dataSourceId) {
        if (sourceEndpointId != null && dataSourceId != null) {
            return taskRepository.findTop50BySourceEndpointIdAndDataSourceIdOrderByStartedAtDesc(sourceEndpointId, dataSourceId);
        }
        if (sourceEndpointId != null) {
            return taskRepository.findTop50BySourceEndpointIdOrderByStartedAtDesc(sourceEndpointId);
        }
        if (dataSourceId != null) {
            return taskRepository.findTop50ByDataSourceIdOrderByStartedAtDesc(dataSourceId);
        }
        return taskRepository.findTop50ByOrderByStartedAtDesc();
    }

    private LineageParseTask createRunningTask(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId, String triggeredBy) {
        LineageParseTask task = new LineageParseTask();
        task.setSourceType(endpoint.getEndpointType());
        task.setSourceEndpointId(endpoint.getId());
        task.setSourceEndpointName(firstNonBlank(endpoint.getDescription(), endpoint.getServiceName(), endpoint.getUrl()));
        task.setDataSourceId(dataSource.getId());
        task.setDataSourceName(dataSource.getName());
        task.setClusterCode(firstNonBlank(dataSource.getClusterCode(), endpoint.getClusterCode()));
        task.setRunId(runId);
        task.setTriggerType("MANUAL");
        task.setTriggeredBy(triggeredBy);
        task.setStatus("RUNNING");
        return taskRepository.save(task);
    }

    private int replaceActiveLineage(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId, LineageCollectResult result) {
        Set<String> savedKeys = new HashSet<>();
        int saved = 0;
        for (ParsedLineageEdge edge : result.getEdges()) {
            TableMetadata source = resolveTable(dataSource.getId(), edge.getSourceDb(), edge.getSourceTable());
            TableMetadata target = resolveTable(dataSource.getId(), edge.getTargetDb(), edge.getTargetTable());
            if (source == null || target == null) {
                result.addFailure("表匹配失败 [" + edgeSource(edge) + "]: "
                        + edge.getSourceDb() + "." + edge.getSourceTable()
                        + " -> " + edge.getTargetDb() + "." + edge.getTargetTable());
                continue;
            }
            String key = source.getId() + "->" + target.getId() + "|" + edge.uniqueKey();
            if (!savedKeys.add(key)) {
                continue;
            }
            DataLineage lineage = new DataLineage();
            lineage.setSourceTableId(source.getId());
            lineage.setTargetTableId(target.getId());
            lineage.setLineageType("ETL");
            lineage.setTransformationLogic(edge.getSql());
            lineage.setSourceType(endpoint.getEndpointType());
            lineage.setSourceEndpointId(endpoint.getId());
            lineage.setDataSourceId(dataSource.getId());
            lineage.setClusterCode(firstNonBlank(dataSource.getClusterCode(), endpoint.getClusterCode()));
            lineage.setSourceProject(edge.getSourceProject());
            lineage.setSourceWorkflow(edge.getSourceWorkflow());
            lineage.setSourceTask(edge.getSourceTask());
            lineage.setSourceTaskKey(edge.getSourceTaskKey());
            lineage.setSourceSqlHash(edge.getSqlHash());
            lineage.setRunId(runId);
            lineage.setStatus("ACTIVE");
            lineage.setParsedAt(LocalDateTime.now());
            lineageRepository.save(lineage);
            saved++;
        }
        lineageRepository.expireActiveBySourceEndpointAndDataSourceExceptRun(endpoint.getId(), dataSource.getId(), runId);
        return saved;
    }

    private String edgeSource(ParsedLineageEdge edge) {
        return firstNonBlank(edge.getSourceProject(), "-")
                + " / " + firstNonBlank(edge.getSourceWorkflow(), "-")
                + " / " + firstNonBlank(edge.getSourceTask(), "-");
    }

    private TableMetadata resolveTable(Long dataSourceId, String dbName, String tableName) {
        List<TableMetadata> tables = tableMetadataRepository.findByDataSourceIdAndDbNameAndTableName(dataSourceId, dbName, tableName);
        return tables.size() == 1 ? tables.get(0) : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String stackMessage(Exception e) {
        StringBuilder builder = new StringBuilder();
        Throwable current = e;
        while (current != null) {
            if (builder.length() > 0) {
                builder.append("\nCaused by: ");
            }
            builder.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
        }
        return builder.toString();
    }
}
