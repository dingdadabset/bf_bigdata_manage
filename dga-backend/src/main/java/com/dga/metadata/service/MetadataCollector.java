package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;

public interface MetadataCollector {
    boolean testConnection(DataSourceConfig config);
    MetadataCollectionResult collectMetadata(DataSourceConfig config);
    MetadataCollectionResult collectTable(DataSourceConfig config, String dbName, String tableName);
    String getType();
}
