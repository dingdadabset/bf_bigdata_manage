package com.dga.access.service;

import com.dga.access.entity.OffboardingRevocationTask;
import com.dga.access.repository.OffboardingRevocationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OffboardingRevocationScheduler {

    @Autowired
    private OffboardingRevocationTaskRepository taskRepository;

    @Autowired
    private OffboardingRevocationService revocationService;

    @Scheduled(
            fixedDelayString = "${dga.offboarding.revocation.scan.fixed-delay-ms:60000}",
            initialDelayString = "${dga.offboarding.revocation.scan.initial-delay-ms:30000}"
    )
    public void scanDueTasks() {
        for (OffboardingRevocationTask task : taskRepository
                .findTop20ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("PENDING", LocalDateTime.now())) {
            revocationService.executeScheduledTask(task.getId());
        }
    }
}
