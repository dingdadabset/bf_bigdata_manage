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
        return getLineageGraph(rootTableId, null, null);
    }

    public Map<String, Object> getLineageGraph(Long rootTableId, String sourceType, Long sourceEndpointId) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. Find the root table
        Optional<TableMetadata> rootTableOpt = tableMetadataRepository.findById(rootTableId);
        if (!rootTableOpt.isPresent()) {
            return result; // Empty result if table not found
        }
        TableMetadata rootTable = rootTableOpt.get();

        // 2. Find immediate upstream (where this table is target)
        List<DataLineage> upstream = lineageRepository.findActiveUpstream(rootTableId, blankToNull(sourceType), sourceEndpointId);
        
        // 3. Find immediate downstream (where this table is source)
        List<DataLineage> downstream = lineageRepository.findActiveDownstream(rootTableId, blankToNull(sourceType), sourceEndpointId);

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
        
        Map<String, Map<String, Object>> linkMap = new LinkedHashMap<>();
        for (DataLineage edge : upstream) {
            addAggregatedLink(linkMap, edge);
        }
        for (DataLineage edge : downstream) {
            addAggregatedLink(linkMap, edge);
        }
        links.addAll(linkMap.values());

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

    @SuppressWarnings("unchecked")
    private void addAggregatedLink(Map<String, Map<String, Object>> linkMap, DataLineage edge) {
        String key = edge.getSourceTableId() + "->" + edge.getTargetTableId();
        Map<String, Object> link = linkMap.computeIfAbsent(key, item -> {
            Map<String, Object> created = new HashMap<>();
            created.put("source", String.valueOf(edge.getSourceTableId()));
            created.put("target", String.valueOf(edge.getTargetTableId()));
            created.put("label", Collections.singletonMap("show", true));
            created.put("sources", new ArrayList<Map<String, Object>>());
            return created;
        });
        List<Map<String, Object>> sources = (List<Map<String, Object>>) link.get("sources");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceType", edge.getSourceType() == null ? "LEGACY" : edge.getSourceType());
        source.put("sourceEndpointId", edge.getSourceEndpointId());
        source.put("project", edge.getSourceProject());
        source.put("workflow", edge.getSourceWorkflow());
        source.put("task", edge.getSourceTask());
        source.put("taskKey", edge.getSourceTaskKey());
        source.put("runId", edge.getRunId());
        source.put("parsedAt", edge.getParsedAt());
        sources.add(source);

        List<String> sourceTypes = sources.stream()
                .map(item -> String.valueOf(item.get("sourceType")))
                .distinct()
                .collect(Collectors.toList());
        link.put("value", String.join(" / ", sourceTypes));
        link.put("sourceCount", sources.size());
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
