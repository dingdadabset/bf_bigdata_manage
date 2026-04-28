package com.dga.metadata.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetadataCollectionResult {

    private int totalTables;
    private int successTables;
    private int failedTables;
    private final List<String> failedDetails = new ArrayList<>();

    public void addSuccess() {
        totalTables++;
        successTables++;
    }

    public void addFailure(String detail) {
        totalTables++;
        failedTables++;
        if (detail != null && failedDetails.size() < 50) {
            failedDetails.add(detail);
        }
    }

    public int getTotalTables() {
        return totalTables;
    }

    public int getSuccessTables() {
        return successTables;
    }

    public int getFailedTables() {
        return failedTables;
    }

    public List<String> getFailedDetails() {
        return Collections.unmodifiableList(failedDetails);
    }

    public String summary() {
        return "total=" + totalTables + ", success=" + successTables + ", failed=" + failedTables;
    }
}
