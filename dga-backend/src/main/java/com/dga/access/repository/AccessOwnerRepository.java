package com.dga.access.repository;

import com.dga.access.entity.AccessOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessOwnerRepository extends JpaRepository<AccessOwner, Long> {

    AccessOwner findByOwnerCodeIgnoreCase(String ownerCode);

    @Query("SELECT o FROM AccessOwner o WHERE o.status <> 'DELETED' AND (" +
            "LOWER(o.ownerCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(o.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(o.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY o.ownerCode ASC")
    List<AccessOwner> searchOwners(@Param("q") String q);
}
