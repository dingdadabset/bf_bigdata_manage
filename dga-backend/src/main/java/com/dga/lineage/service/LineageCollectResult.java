package com.dga.lineage.service;

import java.util.ArrayList;
import java.util.List;

public class LineageCollectResult {

    private final List<ParsedLineageEdge> edges = new ArrayList<>();
    private final List<ParsedSchedulerTaskContext> contexts = new ArrayList<>();
    private final List<ParsedMetadataContextSuggestion> suggestions = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    public List<ParsedLineageEdge> getEdges() {
        return edges;
    }

    public List<String> getFailures() {
        return failures;
    }

    public List<ParsedSchedulerTaskContext> getContexts() {
        return contexts;
    }

    public List<ParsedMetadataContextSuggestion> getSuggestions() {
        return suggestions;
    }

    public void addEdges(List<ParsedLineageEdge> items) {
        if (items != null) {
            edges.addAll(items);
        }
    }

    public void addContexts(List<ParsedSchedulerTaskContext> items) {
        if (items != null) {
            contexts.addAll(items);
        }
    }

    public void addContext(ParsedSchedulerTaskContext item) {
        if (item != null) {
            contexts.add(item);
        }
    }

    public void addSuggestion(ParsedMetadataContextSuggestion item) {
        if (item != null) {
            suggestions.add(item);
        }
    }

    public void addSuggestions(List<ParsedMetadataContextSuggestion> items) {
        if (items != null) {
            suggestions.addAll(items);
        }
    }

    public void addFailure(String failure) {
        if (failure != null && !failure.trim().isEmpty()) {
            failures.add(failure);
        }
    }
}
