package com.dga.metadata.repository;

import com.dga.metadata.entity.TableTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TableTagMappingRepository extends JpaRepository<TableTagMapping, Long> {
    List<TableTagMapping> findByTableId(Long tableId);

    @Transactional
    void deleteByTableId(Long tableId);
}
