package com.dga.resource.repository;

import com.dga.resource.entity.ResourceLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceLinkRepository extends JpaRepository<ResourceLink, Long> {
    List<ResourceLink> findByIsDeletedFalseOrderByRecommendedDescSortOrderAscNameAsc();
    Optional<ResourceLink> findByUrlAndIsDeletedFalse(String url);
}
