package com.dga.quality.repository;

import com.dga.quality.entity.QualityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityRuleRepository extends JpaRepository<QualityRule, Long> {
    List<QualityRule> findByTableId(Long tableId);
}
