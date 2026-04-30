package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.datasource.service.DataSourceSyncService;
import com.dga.metadata.entity.MetadataCollectionTask;
import com.dga.settings.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Date;

@Service
public class MetadataCollectionScheduler implements SchedulingConfigurer {

    @Autowired
    private DataSourceSyncService dataSourceSyncService;

    @Autowired
    private MetadataCollectionService collectionService;

    @Autowired
    private MetadataCollectionAsyncRunner asyncRunner;

    @Autowired
    private SettingsService settingsService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::scheduledCollect, new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                String cron = settingsService.getString("metadataCollection", "collectCron");
                if (cron == null || cron.trim().isEmpty()) {
                    cron = "0 0 2 * * ?";
                }
                try {
                    return new CronTrigger(cron).nextExecutionTime(triggerContext);
                } catch (Exception e) {
                    return new CronTrigger("0 0 2 * * ?").nextExecutionTime(triggerContext);
                }
            }
        });
    }

    public void scheduledCollect() {
        List<DataSourceConfig> dataSources = settingsService.getBoolean("metadataCollection", "autoSyncDataSources", true)
                ? dataSourceSyncService.syncHiveMetastoreDataSources()
                : dataSourceSyncService.getManagedHiveDataSources();
        for (MetadataCollectionTask task : collectionService.createTasksForAll(dataSources, "SCHEDULED", "system")) {
            asyncRunner.run(task.getId());
        }
    }
}
