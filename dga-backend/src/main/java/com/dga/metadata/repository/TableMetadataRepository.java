package com.dga.metadata.repository;

import com.dga.metadata.entity.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface TableMetadataRepository extends JpaRepository<TableMetadata, Long> {
    List<TableMetadata> findByDataSourceId(Long dataSourceId);
    List<TableMetadata> findByDbName(String dbName);

    @Query("SELECT COUNT(DISTINCT t.dbName) FROM TableMetadata t")
    long countDistinctDbName();
}
