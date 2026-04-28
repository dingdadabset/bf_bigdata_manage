package com.dga.metadata.repository;

import com.dga.metadata.entity.DataTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataThemeRepository extends JpaRepository<DataTheme, Long> {
    List<DataTheme> findByStatusOrderBySortOrderAscThemeNameAsc(String status);
}
