package com.dga.metadata.repository;

import com.dga.metadata.entity.MetadataTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetadataTagRepository extends JpaRepository<MetadataTag, Long> {
    Optional<MetadataTag> findByTagName(String tagName);
}
