package com.dga.metadata.repository;

import com.dga.metadata.entity.TableBusinessMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableBusinessMetadataRepository extends JpaRepository<TableBusinessMetadata, Long> {
    Optional<TableBusinessMetadata> findByTableId(Long tableId);
}
