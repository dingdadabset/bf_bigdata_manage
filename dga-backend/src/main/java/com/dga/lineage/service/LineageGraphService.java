package com.dga.lineage.service;

import com.dga.lineage.entity.DataLineage;
import com.dga.lineage.repository.DataLineageRepository;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LineageGraphService {

    @Autowired
    private DataLineageRepository lineageRepository;

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    public Map<String, Object> getLineageGraph(Long rootTableId) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. Find the root table
        Optional<TableMetadata> rootTableOpt = tableMetadataRepository.findById(rootTableId);
        if (!rootTableOpt.isPresent()) {
            return result; // Empty result if table not found
        }
        TableMetadata rootTable = rootTableOpt.get();

        // 2. Find immediate upstream (where this table is target)
        List<DataLineage> upstream = lineageRepository.findByTargetTableId(rootTableId);
        
        // 3. Find immediate downstream (where this table is source)
        List<DataLineage> downstream = lineageRepository.findBySourceTableId(rootTableId);

        // 4. Collect all unique table IDs
        Set<Long> tableIds = new HashSet<>();
        tableIds.add(rootTableId);
        for (DataLineage edge : upstream) {
            tableIds.add(edge.getSourceTableId());
        }
        for (DataLineage edge : downstream) {
            tableIds.add(edge.getTargetTableId());
        }

        // 5. Fetch all table metadata
        List<TableMetadata> tables = tableMetadataRepository.findAllById(tableIds);
        Map<Long, TableMetadata> tableMap = tables.stream()
                .collect(Collectors.toMap(TableMetadata::getId, t -> t));

        // 6. Build Nodes
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Long id : tableIds) {
            TableMetadata meta = tableMap.get(id);
            if (meta != null) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", String.valueOf(id));
                node.put("name", meta.getDbName() + "." + meta.getTableName());
                
                // Determine category/style
                if (id.equals(rootTableId)) {
                    node.put("category", "current");
                    node.put("symbolSize", 30);
                    node.put("itemStyle", Collections.singletonMap("color", "#1890ff"));
                } else if (upstream.stream().anyMatch(e -> e.getSourceTableId().equals(id))) {
                    node.put("category", "upstream");
                    node.put("itemStyle", Collections.singletonMap("color", "#52c41a"));
                } else {
                    node.put("category", "downstream");
                    node.put("itemStyle", Collections.singletonMap("color", "#fa8c16"));
                }
                nodes.add(node);
            }
        }

        // 7. Build Links
        List<Map<String, Object>> links = new ArrayList<>();
        
        // Add upstream links (source -> root)
        for (DataLineage edge : upstream) {
            Map<String, Object> link = new HashMap<>();
            link.put("source", String.valueOf(edge.getSourceTableId()));
            link.put("target", String.valueOf(edge.getTargetTableId()));
            link.put("label", Collections.singletonMap("show", true));
            link.put("value", edge.getLineageType());
            links.add(link);
        }

        // Add downstream links (root -> target)
        for (DataLineage edge : downstream) {
            Map<String, Object> link = new HashMap<>();
            link.put("source", String.valueOf(edge.getSourceTableId()));
            link.put("target", String.valueOf(edge.getTargetTableId()));
            link.put("label", Collections.singletonMap("show", true));
            link.put("value", edge.getLineageType());
            links.add(link);
        }

        result.put("nodes", nodes);
        result.put("links", links);
        
        // Add categories for legend
        List<Map<String, String>> categories = new ArrayList<>();
        categories.add(Collections.singletonMap("name", "current"));
        categories.add(Collections.singletonMap("name", "upstream"));
        categories.add(Collections.singletonMap("name", "downstream"));
        result.put("categories", categories);

        return result;
    }
}
