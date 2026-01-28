package com.dga.metadata.service;

import com.dga.datasource.entity.DataSourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetadataCollectorFactory {

    private final Map<String, MetadataCollector> collectorMap;

    @Autowired
    public MetadataCollectorFactory(List<MetadataCollector> collectors) {
        this.collectorMap = collectors.stream()
                .collect(Collectors.toMap(MetadataCollector::getType, collector -> collector));
    }

    public MetadataCollector getCollector(String type) {
        return collectorMap.get(type.toUpperCase());
    }
}
