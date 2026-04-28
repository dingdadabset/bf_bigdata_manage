package com.dga.metadata.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MetadataCollectionAsyncRunner {

    @Autowired
    private MetadataCollectionService collectionService;

    @Async("metadataTaskExecutor")
    public void run(Long taskId) {
        collectionService.executeTask(taskId);
    }
}
