package com.dga.metadata.repository;

import com.dga.metadata.entity.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, Long> {
    List<ColumnMetadata> findByTableId(Long tableId);
    void deleteByTableId(Long tableId);
}
