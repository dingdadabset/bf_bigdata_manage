package com.dga.access.repository;

import com.dga.access.entity.OffboardingRevocationTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OffboardingRevocationTaskRepository extends JpaRepository<OffboardingRevocationTask, Long> {

    boolean existsByUsernameAndClusterAndStatusIn(String username, String cluster, Collection<String> statuses);

    List<OffboardingRevocationTask> findTop20ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            String status, LocalDateTime scheduledAt);

    List<OffboardingRevocationTask> findTop20ByOrderByCreatedAtDesc();

    List<OffboardingRevocationTask> findByOrderByCreatedAtDesc(Pageable pageable);

    List<OffboardingRevocationTask> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    OffboardingRevocationTask findFirstByUsernameAndClusterAndStatusInOrderByCreatedAtDesc(
            String username, String cluster, Collection<String> statuses);
}
