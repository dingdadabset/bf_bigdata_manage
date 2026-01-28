package com.dga.metadata.service.impl;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.metadata.service.MetadataCollector;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class HiveMetadataCollector implements MetadataCollector {

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try {
            // Load MySQL driver (since Hive Metastore uses MySQL)
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword())) {
                return connection.isValid(5);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void collectMetadata(DataSourceConfig config) {
        // Implement logic to query TBLS, DBS, etc. from MySQL Metastore
        System.out.println("Collecting Hive metadata from " + config.getUrl());
    }

    @Override
    public String getType() {
        return "HIVE";
    }
}
