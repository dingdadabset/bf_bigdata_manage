package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.repository.DataSourceConfigRepository;
import com.dga.metadata.entity.MetadataCollectionTask;
import com.dga.metadata.repository.MetadataCollectionTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataCollectionService {

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private MetadataCollectionTaskRepository taskRepository;

    @Autowired
    private MetadataCollectorFactory collectorFactory;

    @Transactional
    public MetadataCollectionTask createTask(Long dataSourceId, String triggerType, String triggeredBy) {
        DataSourceConfig config = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在: " + dataSourceId));
        if (taskRepository.existsByDataSourceIdAndStatus(dataSourceId, "RUNNING")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该数据源已有采集任务正在运行");
        }

        MetadataCollectionTask task = new MetadataCollectionTask();
        task.setDataSourceId(config.getId());
        task.setDataSourceName(config.getName());
        task.setClusterCode(config.getClusterCode());
        task.setTriggerType(triggerType);
        task.setTriggeredBy(triggeredBy);
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setMessage("元数据采集已启动");
        task = taskRepository.save(task);

        config.setLastSyncStatus("RUNNING");
        config.setLastSyncMessage("元数据采集已启动");
        dataSourceRepository.save(config);
        return task;
    }

    @Transactional
    public void executeTask(Long taskId) {
        MetadataCollectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "采集任务不存在: " + taskId));
        DataSourceConfig config = dataSourceRepository.findById(task.getDataSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据源不存在: " + task.getDataSourceId()));
        try {
            MetadataCollector collector = collectorFactory.getCollector(config.getType());
            if (collector == null) {
                throw new IllegalStateException("不支持的数据源类型: " + config.getType());
            }
            MetadataCollectionResult result = collector.collectMetadata(config);
            task.setStatus(result.getFailedTables() > 0 ? "FAILED" : "SUCCESS");
            task.setSuccessTableCount(result.getSuccessTables());
            task.setFailedTableCount(result.getFailedTables());
            task.setMessage(result.summary());
            task.setErrorDetail(String.join("\n", result.getFailedDetails()));
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

            config.setLastSyncStatus(task.getStatus());
            config.setLastSyncTime(task.getFinishedAt());
            config.setLastSyncMessage(task.getMessage());
            dataSourceRepository.save(config);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setMessage(e.getMessage());
            task.setErrorDetail(stacklessMessage(e));
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

            config.setLastSyncStatus("FAILED");
            config.setLastSyncTime(task.getFinishedAt());
            config.setLastSyncMessage(e.getMessage());
            dataSourceRepository.save(config);
        }
    }

    public List<MetadataCollectionTask> latestTasks() {
        return taskRepository.findTop20ByOrderByStartedAtDesc();
    }

    public MetadataCollectionTask latestTask(Long dataSourceId) {
        return taskRepository.findTopByDataSourceIdOrderByStartedAtDesc(dataSourceId).orElse(null);
    }

    public List<MetadataCollectionTask> createTasksForAll(List<DataSourceConfig> dataSources, String triggerType, String triggeredBy) {
        List<MetadataCollectionTask> tasks = new ArrayList<>();
        for (DataSourceConfig item : dataSources) {
            if (!"HIVE".equalsIgnoreCase(item.getType())) {
                continue;
            }
            if (taskRepository.existsByDataSourceIdAndStatus(item.getId(), "RUNNING")) {
                continue;
            }
            tasks.add(createTask(item.getId(), triggerType, triggeredBy));
        }
        return tasks;
    }

    private String stacklessMessage(Exception e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
