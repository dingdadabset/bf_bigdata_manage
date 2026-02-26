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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HiveMetadataCollector implements MetadataCollector {

    @Autowired
    private TableMetadataRepository tableRepository;

    @Autowired
    private ColumnMetadataRepository columnRepository;

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try {
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
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword())) {
                
                List<String> databases = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT NAME FROM DBS")) {
                    while (rs.next()) {
                        databases.add(rs.getString("NAME"));
                    }
                }

                for (String dbName : databases) {
                    List<String> tables = new ArrayList<>();
                    String tableSql = "SELECT t.TBL_NAME FROM TBLS t JOIN DBS d ON t.DB_ID = d.DB_ID WHERE d.NAME = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(tableSql)) {
                        pstmt.setString(1, dbName);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                tables.add(rs.getString("TBL_NAME"));
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
                    "SELECT t.TBL_ID, t.TBL_NAME, d.NAME as DB_NAME, t.OWNER, t.TBL_TYPE, s.LOCATION, s.INPUT_FORMAT, t.CREATE_TIME " +
                    "FROM TBLS t " +
                    "JOIN DBS d ON t.DB_ID = d.DB_ID " +
                    "LEFT JOIN SDS s ON t.SD_ID = s.SD_ID " +
                    "WHERE d.NAME = ? AND t.TBL_NAME = ?";
                
                PreparedStatement stmt = conn.prepareStatement(tableSql);
                stmt.setString(1, dbName);
                stmt.setString(2, tableName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    long tblId = rs.getLong("TBL_ID");
                    String owner = rs.getString("OWNER");
                    String location = rs.getString("LOCATION");
                    String inputFormat = rs.getString("INPUT_FORMAT");
                    long createTime = rs.getLong("CREATE_TIME");
                    
                    // Determine storage format
                    String format = "TEXTFILE";
                    if (inputFormat != null) {
                        if (inputFormat.contains("Orc")) format = "ORC";
                        else if (inputFormat.contains("Parquet")) format = "PARQUET";
                        else if (inputFormat.contains("Avro")) format = "AVRO";
                    }

                    // Fetch Table Params (totalSize, numRows)
                    long totalSize = 0;
                    long numRows = 0;
                    try (PreparedStatement paramStmt = conn.prepareStatement("SELECT PARAM_KEY, PARAM_VALUE FROM TABLE_PARAMS WHERE TBL_ID = ?")) {
                        paramStmt.setLong(1, tblId);
                        try (ResultSet paramRs = paramStmt.executeQuery()) {
                            while (paramRs.next()) {
                                String key = paramRs.getString("PARAM_KEY");
                                String value = paramRs.getString("PARAM_VALUE");
                                if ("totalSize".equals(key)) {
                                    totalSize = Long.parseLong(value);
                                } else if ("numRows".equals(key)) {
                                    numRows = Long.parseLong(value);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore param fetch errors
                    }

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

                    // Check for changes
                    if (!fieldEquals(tableMetadata.getOwner(), owner)) {
                        tableMetadata.setOwner(owner);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getLocationPath(), location)) {
                        tableMetadata.setLocationPath(location);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getStorageFormat(), format)) {
                        tableMetadata.setStorageFormat(format);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getTotalSize(), totalSize)) {
                        tableMetadata.setTotalSize(totalSize);
                        changed = true;
                    }
                    if (!fieldEquals(tableMetadata.getRecordCount(), numRows)) {
                        tableMetadata.setRecordCount(numRows);
                        changed = true;
                    }

                    // Calculate Governance Score
                    double score = 0.0;
                    if (owner != null && !owner.isEmpty()) score += 40.0;
                    if (!"TEXTFILE".equals(format)) score += 30.0;
                    if (totalSize > 0) score += 30.0;
                    
                    if (!fieldEquals(tableMetadata.getGovernanceScore(), score)) {
                        tableMetadata.setGovernanceScore(score);
                        changed = true;
                    }
                    
                    // Only save if changed or new
                    if (changed) {
                        tableMetadata.setSyncTime(LocalDateTime.now());
                        tableMetadata = tableRepository.save(tableMetadata);
                    } else {
                        // Even if no changes, update sync time
                        tableMetadata.setSyncTime(LocalDateTime.now());
                        tableMetadata = tableRepository.save(tableMetadata);
                    }

                    // 3. Fetch Columns
                    String colSql = 
                        "SELECT c.COLUMN_NAME, c.TYPE_NAME, c.COMMENT, c.INTEGER_IDX " +
                        "FROM COLUMNS_V2 c " +
                        "JOIN SDS s ON s.CD_ID = c.CD_ID " +
                        "JOIN TBLS t ON t.SD_ID = s.SD_ID " +
                        "WHERE t.TBL_ID = ? " +
                        "ORDER BY c.INTEGER_IDX";
                    
                    try (PreparedStatement colStmt = conn.prepareStatement(colSql)) {
                        colStmt.setLong(1, tblId);
                        try (ResultSet colRs = colStmt.executeQuery()) {
                            // Fetch existing columns once
                            List<ColumnMetadata> existingCols = columnRepository.findByTableId(tableMetadata.getId());
                            Map<String, ColumnMetadata> colMap = existingCols.stream()
                                .collect(Collectors.toMap(ColumnMetadata::getColumnName, c -> c));

                            boolean columnsChanged = false;

                            while (colRs.next()) {
                                String colName = colRs.getString("COLUMN_NAME");
                                String colType = colRs.getString("TYPE_NAME");
                                String colComment = colRs.getString("COMMENT");
                                int colIdx = colRs.getInt("INTEGER_IDX");

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
                                // You might want to store column index/position if your entity supports it

                                if (colChanged || colNew) {
                                    columnRepository.save(colMeta);
                                    columnsChanged = true;
                                }
                                
                                // Remove from map to track deleted columns
                                colMap.remove(colName);
                            }

                            // Optional: Handle deleted columns (remaining in colMap)
                            // for (ColumnMetadata deletedCol : colMap.values()) {
                            //    columnRepository.delete(deletedCol);
                            //    columnsChanged = true;
                            // }
                            
                            if (columnsChanged && !changed) {
                                tableMetadata.setUpdatedAt(LocalDateTime.now());
                                tableRepository.save(tableMetadata);
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
        return "HIVE";
    }
}
