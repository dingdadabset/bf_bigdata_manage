package com.dga.access.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DgaUserSchemaService {

    private static final String TABLE_NAME = "dga_users";
    private static final String DEFAULT_LEGACY_CLUSTER = "CDH-Cluster-01";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public synchronized void ensureClusterScopedUsernameConstraint() {
        if (!tableExists()) {
            return;
        }
        ensureClusterNameColumn();
        repairLegacyClusterNames();
        dropGlobalUsernameUniqueIndexes();
        ensureClusterUsernameUniqueIndex();
    }

    private boolean tableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                TABLE_NAME
        );
        return count != null && count > 0;
    }

    private void ensureClusterNameColumn() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'cluster_name'",
                Integer.class,
                TABLE_NAME
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE dga_users ADD COLUMN cluster_name VARCHAR(255)");
        }
    }

    private void repairLegacyClusterNames() {
        jdbcTemplate.update(
                "UPDATE dga_users SET cluster_name = ? WHERE cluster_name IS NULL OR cluster_name = ''",
                DEFAULT_LEGACY_CLUSTER
        );
    }

    private void dropGlobalUsernameUniqueIndexes() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_text " +
                        "FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? " +
                        "AND NON_UNIQUE = 0 " +
                        "AND INDEX_NAME <> 'PRIMARY' " +
                        "GROUP BY INDEX_NAME",
                TABLE_NAME
        );
        for (Map<String, Object> index : indexes) {
            String indexName = String.valueOf(index.get("INDEX_NAME"));
            String columnsText = String.valueOf(index.get("columns_text"));
            if ("username".equalsIgnoreCase(columnsText)) {
                jdbcTemplate.execute("ALTER TABLE dga_users DROP INDEX `" + indexName + "`");
            }
        }
    }

    private void ensureClusterUsernameUniqueIndex() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_text " +
                        "FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? " +
                        "AND NON_UNIQUE = 0 " +
                        "AND INDEX_NAME <> 'PRIMARY' " +
                        "GROUP BY INDEX_NAME",
                TABLE_NAME
        );
        for (Map<String, Object> index : indexes) {
            String columnsText = String.valueOf(index.get("columns_text"));
            if ("cluster_name,username".equalsIgnoreCase(columnsText)) {
                return;
            }
        }
        jdbcTemplate.execute("ALTER TABLE dga_users ADD UNIQUE KEY uk_cluster_username (cluster_name, username)");
    }
}
