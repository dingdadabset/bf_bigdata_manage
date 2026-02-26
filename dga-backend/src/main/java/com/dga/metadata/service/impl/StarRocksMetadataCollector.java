package com.dga.metadata.service.impl;

import com.dga.datasource.entity.DataSourceConfig;
import com.dga.metadata.entity.ColumnMetadata;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.ColumnMetadataRepository;
import com.dga.metadata.repository.TableMetadataRepository;
import com.dga.metadata.service.MetadataCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StarRocksMetadataCollector implements MetadataCollector {

    @Autowired
    private TableMetadataRepository tableRepository;

    @Autowired
    private ColumnMetadataRepository columnRepository;

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
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword())) {
                
                List<String> databases = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', '_statistics_', 'mysql', 'sys', 'performance_schema')")) {
                    while (rs.next()) {
                        databases.add(rs.getString("SCHEMA_NAME"));
                    }
                }

                for (String dbName : databases) {
                    List<String> tables = new ArrayList<>();
                    String tableSql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(tableSql)) {
                        pstmt.setString(1, dbName);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                tables.add(rs.getString("TABLE_NAME"));
                            }
                        }
                    }

                    for (String tableName : tables) {
                        try {
                            collectTableMetadata(conn, config, dbName, tableName);
                        } catch (Exception e) {
                            System.err.println("Error collecting table " + dbName + "." + tableName + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to collect metadata: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void collectTable(DataSourceConfig config, String dbName, String tableName) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword())) {
                collectTableMetadata(conn, config, dbName, tableName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to collect table metadata: " + e.getMessage());
        }
    }

    private void collectTableMetadata(Connection conn, DataSourceConfig config, String dbName, String tableName) throws SQLException {
        // 1. Fetch Table Info
        String tableSql = 
            "SELECT TABLE_NAME, TABLE_COMMENT, TABLE_TYPE, CREATE_TIME, UPDATE_TIME, DATA_LENGTH, TABLE_ROWS, ENGINE " +
            "FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(tableSql)) {
            stmt.setString(1, dbName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String comment = rs.getString("TABLE_COMMENT");
                    String type = rs.getString("TABLE_TYPE"); // BASE TABLE or VIEW
                    // Timestamp createTime = rs.getTimestamp("CREATE_TIME");
                    // Timestamp updateTime = rs.getTimestamp("UPDATE_TIME");
                    long dataLength = rs.getLong("DATA_LENGTH");
                    long tableRows = rs.getLong("TABLE_ROWS");
                    String engine = rs.getString("ENGINE");

                    // 2. Find existing or create new
                    Optional<TableMetadata> existingOpt = tableRepository.findOne((root, query, cb) -> 
                        cb.and(
                            cb.equal(root.get("dataSourceId"), config.getId()),
                            cb.equal(root.get("dbName"), dbName),
                            cb.equal(root.get("tableName"), tableName)
                        )
                    );

                    TableMetadata tableMetadata = existingOpt.orElse(new TableMetadata());
                    boolean isNew = !existingOpt.isPresent();
                    boolean changed = false;

                    if (isNew) {
                        tableMetadata.setDataSourceId(config.getId());
                        tableMetadata.setDbName(dbName);
                        tableMetadata.setTableName(tableName);
                        changed = true;
                    }

                    // For StarRocks, owner might not be easily available, skipping
                    // Storage Format -> Engine
                    if (!fieldEquals(tableMetadata.getStorageFormat(), engine)) {
                        tableMetadata.setStorageFormat(engine);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getTotalSize(), dataLength)) {
                        tableMetadata.setTotalSize(dataLength);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getRecordCount(), tableRows)) {
                        tableMetadata.setRecordCount(tableRows);
                        changed = true;
                    }

                    // Calculate Governance Score
                    double score = 0.0;
                    if (comment != null && !comment.isEmpty()) score += 20.0;
                    if (tableRows > 0) score += 40.0;
                    if (dataLength > 0) score += 40.0;
                    
                    if (!fieldEquals(tableMetadata.getGovernanceScore(), score)) {
                        tableMetadata.setGovernanceScore(score);
                        changed = true;
                    }
                    
                    if (changed) {
                        tableMetadata.setSyncTime(LocalDateTime.now());
                        tableMetadata = tableRepository.save(tableMetadata);
                    } else {
                        tableMetadata.setSyncTime(LocalDateTime.now());
                        tableMetadata = tableRepository.save(tableMetadata);
                    }

                    // 3. Fetch Columns
                    String colSql = 
                        "SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, ORDINAL_POSITION, COLUMN_KEY " +
                        "FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                        "ORDER BY ORDINAL_POSITION";
                    
                    try (PreparedStatement colStmt = conn.prepareStatement(colSql)) {
                        colStmt.setString(1, dbName);
                        colStmt.setString(2, tableName);
                        try (ResultSet colRs = colStmt.executeQuery()) {
                            
                            List<ColumnMetadata> existingCols = columnRepository.findByTableId(tableMetadata.getId());
                            Map<String, ColumnMetadata> colMap = existingCols.stream()
                                .collect(Collectors.toMap(ColumnMetadata::getColumnName, c -> c));

                            boolean columnsChanged = false;

                            while (colRs.next()) {
                                String colName = colRs.getString("COLUMN_NAME");
                                String colType = colRs.getString("COLUMN_TYPE");
                                String colComment = colRs.getString("COLUMN_COMMENT");
                                // String colKey = colRs.getString("COLUMN_KEY"); // PRI, UNI, MUL

                                ColumnMetadata colMeta = colMap.get(colName);
                                boolean colNew = (colMeta == null);
                                boolean colChanged = false;

                                if (colNew) {
                                    colMeta = new ColumnMetadata();
                                    colMeta.setTableId(tableMetadata.getId());
                                    colMeta.setColumnName(colName);
                                    colChanged = true;
                                }

                                if (!fieldEquals(colMeta.getColumnType(), colType)) {
                                    colMeta.setColumnType(colType);
                                    colChanged = true;
                                }
                                if (!fieldEquals(colMeta.getComment(), colComment)) {
                                    colMeta.setComment(colComment);
                                    colChanged = true;
                                }

                                if (colChanged || colNew) {
                                    columnRepository.save(colMeta);
                                    columnsChanged = true;
                                }
                                
                                colMap.remove(colName);
                            }
                            
                            if (columnsChanged && !changed) {
                                tableMetadata.setUpdatedAt(LocalDateTime.now());
                                tableRepository.save(tableMetadata);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean fieldEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    @Override
    public String getType() {
        return "STARROCKS";
    }
}
