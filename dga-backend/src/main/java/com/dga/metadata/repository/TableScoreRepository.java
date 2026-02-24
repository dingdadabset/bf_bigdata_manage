package com.dga.metadata.repository;

import com.dga.metadata.entity.TableScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TableScoreRepository extends JpaRepository<TableScore, Long> {
    Optional<TableScore> findByTableIdAndReportDate(Long tableId, LocalDate reportDate);
}
