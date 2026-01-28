package com.dga.metadata.controller;

import com.dga.metadata.entity.TableMetadata;
import com.dga.metadata.repository.TableMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    @Autowired
    private TableMetadataRepository tableMetadataRepository;

    @GetMapping("/tables")
    public List<TableMetadata> getTables(@RequestParam(required = false) Long dataSourceId) {
        if (dataSourceId != null) {
            return tableMetadataRepository.findByDataSourceId(dataSourceId);
        }
        return tableMetadataRepository.findAll();
    }
    
    @PostMapping("/sync")
    public String syncMetadata() {
        // Mock sync logic
        return "Metadata sync started";
    }
}
