package com.dga.quality.controller;

import com.dga.quality.entity.QualityExecution;
import com.dga.quality.entity.QualityRule;
import com.dga.quality.repository.QualityExecutionRepository;
import com.dga.quality.repository.QualityRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    @Autowired
    private QualityRuleRepository ruleRepository;

    @Autowired
    private QualityExecutionRepository executionRepository;

    @GetMapping("/rules")
    public List<QualityRule> getRules() {
        return ruleRepository.findAll();
    }

    @PostMapping("/rules")
    public QualityRule createRule(@RequestBody QualityRule rule) {
        return ruleRepository.save(rule);
    }

    @PostMapping("/execute/{ruleId}")
    public QualityExecution executeRule(@PathVariable Long ruleId) {
        QualityExecution execution = new QualityExecution();
        execution.setRuleId(ruleId);
        
        // Mock execution logic
        boolean success = new Random().nextBoolean();
        execution.setStatus(success ? "SUCCESS" : "FAILED");
        execution.setResultValue(success ? 0.0 : 0.1); // 0% error or 10% error
        execution.setExecutedAt(LocalDateTime.now());
        
        return executionRepository.save(execution);
    }
    
    @GetMapping("/executions")
    public List<QualityExecution> getExecutions(@RequestParam(required = false) Long ruleId) {
        if (ruleId != null) {
            return executionRepository.findByRuleIdOrderByExecutedAtDesc(ruleId);
        }
        return executionRepository.findAll();
    }
}
