package com.dga.metadata.service.impl;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.metadata.service.MetadataCollector;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class StarRocksMetadataCollector implements MetadataCollector {

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // StarRocks supports MySQL protocol
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
        System.out.println("Collecting StarRocks metadata from " + config.getUrl());
    }

    @Override
    public String getType() {
        return "STARROCKS";
    }
}
