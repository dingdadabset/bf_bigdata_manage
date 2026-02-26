package com.dga.metadata.repository;

import com.dga.metadata.entity.TableMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long>, JpaSpecificationExecutor<TableMetadata> {
    List<TableMetadata> findByDataSourceId(Long dataSourceId);
    List<TableMetadata> findByDbName(String dbName);

    Page<TableMetadata> findByDataSourceId(Long dataSourceId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT t.dbName) FROM TableMetadata t")
    long countDistinctDbName();
    @Query("SELECT SUM(COALESCE(t.totalSize, 0)) FROM TableMetadata t")
    Long sumTotalSize();

    @Query("SELECT AVG(t.governanceScore) FROM TableMetadata t")
    Double avgGovernanceScore();

    long countBySyncTimeAfter(LocalDateTime timestamp);
}
