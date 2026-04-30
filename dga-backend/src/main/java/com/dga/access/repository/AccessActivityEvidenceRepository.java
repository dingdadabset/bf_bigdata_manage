package com.dga.access.repository;

import com.dga.access.entity.AccessActivityEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface AccessActivityEvidenceRepository extends JpaRepository<AccessActivityEvidence, Long> {

    AccessActivityEvidence findByEvidenceKey(String evidenceKey);

    @Query("SELECT e FROM AccessActivityEvidence e WHERE e.username IN :usernames " +
            "AND e.sourceSystem IN :sources " +
            "AND (:cluster IS NULL OR :cluster = '' OR e.clusterCode = :cluster OR e.clusterName = :cluster)")
    List<AccessActivityEvidence> findEvidenceForUsers(@Param("usernames") Collection<String> usernames,
                                                      @Param("sources") Collection<String> sources,
                                                      @Param("cluster") String cluster);

    @Query("SELECT COUNT(e) FROM AccessActivityEvidence e " +
            "WHERE (:cluster IS NULL OR :cluster = '' OR e.clusterCode = :cluster OR e.clusterName = :cluster)")
    long countByCluster(@Param("cluster") String cluster);

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessActivityEvidence e " +
            "WHERE (:cluster IS NULL OR :cluster = '' OR e.clusterCode = :cluster OR e.clusterName = :cluster)")
    int deleteByCluster(@Param("cluster") String cluster);
}
