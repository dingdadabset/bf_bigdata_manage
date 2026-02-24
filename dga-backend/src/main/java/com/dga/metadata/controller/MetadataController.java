package com.dga.metadata.controller;

import com.dga.metadata.entity.ColumnMetadata;
import com.dga.metadata.entity.GovernanceTask;
import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.ColumnMetadataRepository;
import com.dga.metadata.repository.GovernanceTaskRepository;
import com.dga.metadata.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @Autowired
    private ColumnMetadataRepository columnMetadataRepository;

    @Autowired
    private GovernanceTaskRepository governanceTaskRepository;

    @GetMapping("/tables")
    public List<TableMetadata> getTables(@RequestParam(required = false) Long dataSourceId) {
        if (dataSourceId != null) {
            return tableMetadataRepository.findByDataSourceId(dataSourceId);
        }
        return tableMetadataRepository.findAll();
    }

    @GetMapping("/tables/page")
    public Page<TableMetadata> getTablesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long dataSourceId,
            @RequestParam(required = false) String dbName,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String keyword) {
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        Pageable pageable = PageRequest.of(page, size);

        return tableMetadataRepository.findAll((Specification<TableMetadata>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (dataSourceId != null) {
                predicates.add(cb.equal(root.get("dataSourceId"), dataSourceId));
            }
            if (dbName != null && !dbName.isEmpty()) {
                predicates.add(cb.equal(root.get("dbName"), dbName));
            }
            if (owner != null && !owner.isEmpty()) {
                predicates.add(cb.equal(root.get("owner"), owner));
            }
            if (keyword != null && !keyword.isEmpty()) {
                String likePattern = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("tableName"), likePattern),
                    cb.like(root.get("dbName"), likePattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @GetMapping("/table/{id}")
    public TableMetadata getTable(@PathVariable Long id) {
        return tableMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
    }

    @GetMapping("/table/{id}/columns")
    public List<ColumnMetadata> getColumns(@PathVariable Long id) {
        return columnMetadataRepository.findByTableId(id);
    }

    @GetMapping("/table/{id}/governance-tasks")
    public List<GovernanceTask> getGovernanceTasks(@PathVariable Long id) {
        return governanceTaskRepository.findByTableId(id);
    }

    @PostMapping("/sync")
    public String syncMetadata() {
        return "Metadata sync started";
    }
}
