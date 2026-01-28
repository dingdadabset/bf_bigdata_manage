package com.dga.datasource.repository;

import com.dga.datasource.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {
}
