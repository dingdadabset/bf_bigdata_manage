package com.dga.quality.controller;

import com.dga.access.service.AdminGuard;
import com.dga.quality.entity.QualityExecution;
import com.dga.quality.entity.QualityIssue;
import com.dga.quality.entity.QualityRule;
import com.dga.quality.repository.QualityExecutionRepository;
import com.dga.quality.repository.QualityIssueRepository;
import com.dga.quality.repository.QualityRuleRepository;
import com.dga.quality.service.QualityExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    @Autowired
    private QualityRuleRepository ruleRepository;

    @Autowired
    private QualityExecutionRepository executionRepository;

    @Autowired
    private QualityIssueRepository issueRepository;

    @Autowired
    private QualityExecutionService qualityService;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<QualityExecution> recentExecutions = executionRepository.findTop100ByOrderByExecutedAtDesc();
        long successCount = recentExecutions.stream().filter(item -> "SUCCESS".equals(item.getStatus())).count();
        double recentSuccessRate = recentExecutions.isEmpty() ? 0.0 : Math.round((successCount * 1000.0 / recentExecutions.size())) / 10.0;

        result.put("totalRules", ruleRepository.count());
        result.put("activeRules", ruleRepository.countByStatus("ACTIVE"));
        result.put("failedRules", ruleRepository.countByLastExecutionStatus("FAILED"));
        result.put("openIssues", issueRepository.countByStatus("OPEN"));
        result.put("recentSuccessRate", recentSuccessRate);
        result.put("recentExecutionCount", recentExecutions.size());
        return result;
    }

    @GetMapping("/rules")
    public List<QualityRule> getRules(@RequestParam(required = false) Long dataSourceId,
                                      @RequestParam(required = false) Long tableId,
                                      @RequestParam(required = false) String status) {
        return qualityService.findRules(dataSourceId, tableId, status);
    }

    @PostMapping("/rules")
    public QualityRule createRule(@RequestBody QualityRule rule, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护质量规则");
        return qualityService.createRule(rule);
    }

    @PutMapping("/rules/{id}")
    public QualityRule updateRule(@PathVariable Long id,
                                  @RequestBody QualityRule rule,
                                  HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护质量规则");
        return qualityService.updateRule(id, rule);
    }

    @DeleteMapping("/rules/{id}")
    public void deleteRule(@PathVariable Long id, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可维护质量规则");
        qualityService.deleteRule(id);
    }

    @PostMapping("/execute/{ruleId}")
    public QualityExecution executeRule(@PathVariable Long ruleId, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可执行质量规则");
        return qualityService.executeRule(ruleId);
    }

    @PostMapping("/execute/table/{tableId}")
    public List<QualityExecution> executeTable(@PathVariable Long tableId, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可执行质量规则");
        return qualityService.executeTable(tableId);
    }
    
    @GetMapping("/executions")
    public Page<QualityExecution> getExecutions(@RequestParam(required = false) Long ruleId,
                                                @RequestParam(required = false) Long tableId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "executedAt", "id"));
        return executionRepository.findAll((Specification<QualityExecution>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ruleId != null) {
                predicates.add(cb.equal(root.get("ruleId"), ruleId));
            }
            if (tableId != null) {
                predicates.add(cb.equal(root.get("tableId"), tableId));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @GetMapping("/issues")
    public Page<QualityIssue> getIssues(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long tableId,
                                        @RequestParam(required = false) String owner,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "lastSeenAt", "id"));
        return issueRepository.findAll((Specification<QualityIssue>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (tableId != null) {
                predicates.add(cb.equal(root.get("tableId"), tableId));
            }
            if (owner != null && !owner.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("owner"), owner.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
}
