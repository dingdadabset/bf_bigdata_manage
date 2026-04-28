package com.dga.lineage.service;

import java.util.ArrayList;
import java.util.List;

public class LineageCollectResult {

    private final List<ParsedLineageEdge> edges = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    public List<ParsedLineageEdge> getEdges() {
        return edges;
    }

    public List<String> getFailures() {
        return failures;
    }

    public void addEdges(List<ParsedLineageEdge> items) {
        if (items != null) {
            edges.addAll(items);
        }
    }

    public void addFailure(String failure) {
        if (failure != null && !failure.trim().isEmpty()) {
            failures.add(failure);
        }
    }
}
