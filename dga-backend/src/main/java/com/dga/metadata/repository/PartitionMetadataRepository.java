package com.dga.metadata.repository;

import com.dga.metadata.entity.PartitionMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PartitionMetadataRepository extends JpaRepository<PartitionMetadata, Long> {
    List<PartitionMetadata> findByTableIdOrderByLastModifyTimeDescIdDesc(Long tableId);

    List<PartitionMetadata> findByTableIdOrderByLastModifyTimeDescIdDesc(Long tableId, Pageable pageable);

    @Transactional
    void deleteByTableId(Long tableId);
}
