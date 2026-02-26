package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;

public interface MetadataCollector {
    boolean testConnection(DataSourceConfig config);
    // Placeholder for actual collection logic
    void collectMetadata(DataSourceConfig config);
    void collectTable(DataSourceConfig config, String dbName, String tableName);
    String getType();
}
