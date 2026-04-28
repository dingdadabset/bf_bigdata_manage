package com.dga.metadata.repository;

import com.dga.metadata.entity.TableMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long>, JpaSpecificationExecutor<TableMetadata> {
    List<TableMetadata> findByDataSourceId(Long dataSourceId);
    List<TableMetadata> findByClusterCode(String clusterCode);
    List<TableMetadata> findByDbName(String dbName);
    List<TableMetadata> findByDbNameAndTableName(String dbName, String tableName);
    List<TableMetadata> findByDataSourceIdAndDbNameAndTableName(Long dataSourceId, String dbName, String tableName);
    List<TableMetadata> findByClusterCodeAndDbNameAndTableName(String clusterCode, String dbName, String tableName);

    Page<TableMetadata> findByDataSourceId(Long dataSourceId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT t.dbName) FROM TableMetadata t")
    long countDistinctDbName();
    @Query("SELECT SUM(COALESCE(t.totalSize, 0)) FROM TableMetadata t")
    Long sumTotalSize();

    @Query("SELECT AVG(t.governanceScore) FROM TableMetadata t")
    Double avgGovernanceScore();

    long countBySyncTimeAfter(LocalDateTime timestamp);

    @Query(value = "SELECT DISTINCT t.* FROM meta_table_info t " +
            "LEFT JOIN meta_column_info c ON c.table_id = t.id " +
            "LEFT JOIN dga_table_business_metadata b ON b.table_id = t.id " +
            "LEFT JOIN dga_data_theme th ON th.id = b.theme_id " +
            "LEFT JOIN dga_table_tag_mapping tm ON tm.table_id = t.id " +
            "LEFT JOIN dga_metadata_tag tag ON tag.id = tm.tag_id " +
            "LEFT JOIN dga_metric_definition m ON m.table_id = t.id " +
            "WHERE (:dataSourceId IS NULL OR t.datasource_id = :dataSourceId) " +
            "AND (:dbName IS NULL OR :dbName = '' OR t.db_name = :dbName) " +
            "AND (:owner IS NULL OR :owner = '' OR t.owner = :owner OR b.business_owner = :owner) " +
            "AND (:lifecycleStatus IS NULL OR :lifecycleStatus = '' OR t.lifecycle_status = :lifecycleStatus OR (:lifecycleStatus = 'ONLINE' AND t.lifecycle_status IS NULL)) " +
            "AND (:themeId IS NULL OR b.theme_id = :themeId) " +
            "AND (:tagId IS NULL OR tm.tag_id = :tagId) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.db_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.table_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(t.table_comment, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(t.owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(t.source_owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(c.column_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(c.column_comment, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(b.business_description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(b.business_definition, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(b.business_owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(th.theme_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(tag.tag_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(m.metric_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(m.metric_code, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(m.business_definition, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) ",
            countQuery = "SELECT COUNT(DISTINCT t.id) FROM meta_table_info t " +
                    "LEFT JOIN meta_column_info c ON c.table_id = t.id " +
                    "LEFT JOIN dga_table_business_metadata b ON b.table_id = t.id " +
                    "LEFT JOIN dga_data_theme th ON th.id = b.theme_id " +
                    "LEFT JOIN dga_table_tag_mapping tm ON tm.table_id = t.id " +
                    "LEFT JOIN dga_metadata_tag tag ON tag.id = tm.tag_id " +
                    "LEFT JOIN dga_metric_definition m ON m.table_id = t.id " +
                    "WHERE (:dataSourceId IS NULL OR t.datasource_id = :dataSourceId) " +
                    "AND (:dbName IS NULL OR :dbName = '' OR t.db_name = :dbName) " +
                    "AND (:owner IS NULL OR :owner = '' OR t.owner = :owner OR b.business_owner = :owner) " +
                    "AND (:lifecycleStatus IS NULL OR :lifecycleStatus = '' OR t.lifecycle_status = :lifecycleStatus OR (:lifecycleStatus = 'ONLINE' AND t.lifecycle_status IS NULL)) " +
                    "AND (:themeId IS NULL OR b.theme_id = :themeId) " +
                    "AND (:tagId IS NULL OR tm.tag_id = :tagId) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "LOWER(t.db_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(t.table_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(t.table_comment, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(t.owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(t.source_owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(c.column_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(c.column_comment, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(b.business_description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(b.business_definition, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(b.business_owner, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(th.theme_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(tag.tag_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(m.metric_name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(m.metric_code, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(COALESCE(m.business_definition, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) ",
            nativeQuery = true)
    Page<TableMetadata> searchAssets(@Param("keyword") String keyword,
                                     @Param("dataSourceId") Long dataSourceId,
                                     @Param("dbName") String dbName,
                                     @Param("owner") String owner,
                                     @Param("themeId") Long themeId,
                                     @Param("tagId") Long tagId,
                                     @Param("lifecycleStatus") String lifecycleStatus,
                                     Pageable pageable);
}
