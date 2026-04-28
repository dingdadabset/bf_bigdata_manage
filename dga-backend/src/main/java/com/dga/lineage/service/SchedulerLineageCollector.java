package com.dga.lineage.service;

import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.datasource.entity.DataSourceConfig;

public interface SchedulerLineageCollector {
    boolean supports(String endpointType);

    LineageCollectResult collect(ClusterEndpoint endpoint, DataSourceConfig dataSource, String runId);
}
