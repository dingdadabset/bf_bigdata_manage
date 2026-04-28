package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.service.DataSourceSyncService;
import com.dga.metadata.entity.MetadataCollectionTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetadataCollectionScheduler {

    @Autowired
    private DataSourceSyncService dataSourceSyncService;

    @Autowired
    private MetadataCollectionService collectionService;

    @Autowired
    private MetadataCollectionAsyncRunner asyncRunner;

    @Scheduled(cron = "${dga.metadata.collect.cron:0 0 2 * * ?}")
    public void scheduledCollect() {
        List<DataSourceConfig> dataSources = dataSourceSyncService.syncHiveMetastoreDataSources();
        for (MetadataCollectionTask task : collectionService.createTasksForAll(dataSources, "SCHEDULED", "system")) {
            asyncRunner.run(task.getId());
        }
    }
}
