package com.dga.metadata.service;

import com.dga.metadata.entity.ColumnMetadata;
import com.dga.metadata.entity.MetadataContextSuggestion;
import com.dga.metadata.entity.TableBusinessMetadata;
import com.dga.metadata.repository.ColumnMetadataRepository;
import com.dga.metadata.repository.MetadataContextSuggestionRepository;
import com.dga.metadata.repository.TableBusinessMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetadataContextSuggestionService {
    @Autowired
    private MetadataContextSuggestionRepository suggestionRepository;

    @Autowired
    private ColumnMetadataRepository columnRepository;

    @Autowired
    private TableBusinessMetadataRepository businessRepository;

    public List<MetadataContextSuggestion> listByTable(Long tableId) {
        return suggestionRepository.findByTableIdOrderByStatusAscParsedAtDescIdDesc(tableId);
    }

    @Transactional
    public MetadataContextSuggestion apply(Long id, String operator) {
        MetadataContextSuggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上下文建议不存在: " + id));
        if (!"PENDING".equals(suggestion.getStatus())) {
            return suggestion;
        }
        if ("COLUMN_COMMENT".equals(suggestion.getContextType())) {
            applyColumnComment(suggestion);
        } else if ("SCHEDULER_CONTEXT".equals(suggestion.getContextType())) {
            applyTableContext(suggestion, operator);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的建议类型: " + suggestion.getContextType());
        }
        suggestion.setStatus("APPLIED");
        suggestion.setAppliedAt(LocalDateTime.now());
        suggestion.setAppliedBy(operator);
        return suggestionRepository.save(suggestion);
    }

    @Transactional
    public MetadataContextSuggestion reject(Long id, String operator) {
        MetadataContextSuggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上下文建议不存在: " + id));
        suggestion.setStatus("REJECTED");
        suggestion.setAppliedAt(LocalDateTime.now());
        suggestion.setAppliedBy(operator);
        return suggestionRepository.save(suggestion);
    }

    private void applyColumnComment(MetadataContextSuggestion suggestion) {
        if (suggestion.getColumnId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段建议缺少 columnId，无法应用");
        }
        ColumnMetadata column = columnRepository.findById(suggestion.getColumnId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "字段不存在: " + suggestion.getColumnId()));
        column.setComment(suggestion.getSuggestedValue());
        columnRepository.save(column);
    }

    private void applyTableContext(MetadataContextSuggestion suggestion, String operator) {
        TableBusinessMetadata business = businessRepository.findByTableId(suggestion.getTableId()).orElseGet(() -> {
            TableBusinessMetadata item = new TableBusinessMetadata();
            item.setTableId(suggestion.getTableId());
            return item;
        });
        String value = suggestion.getSuggestedValue();
        String current = business.getBusinessDescription();
        if (current == null || current.trim().isEmpty()) {
            business.setBusinessDescription(value);
        } else if (value != null && !current.contains(value)) {
            business.setBusinessDescription(current + "\n" + value);
        }
        business.setUpdatedBy(operator);
        businessRepository.save(business);
    }
}
