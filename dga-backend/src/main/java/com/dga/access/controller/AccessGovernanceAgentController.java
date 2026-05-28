package com.dga.access.controller;

import com.dga.access.dto.GovernanceAgentRequest;
import com.dga.access.service.AccessGovernanceAgentService;
import com.dga.access.service.AdminGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/access/governance/agent")
public class AccessGovernanceAgentController {

    @Autowired
    private AccessGovernanceAgentService agentService;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping("/config/status")
    public Map<String, Object> configStatus() {
        return agentService.configStatus();
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody GovernanceAgentRequest request, HttpServletRequest httpRequest, Principal principal) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可使用风险治理 AI 分析");
        return agentService.analyze(request, currentOperator(principal));
    }

    @PostMapping("/plan")
    public Map<String, Object> plan(@RequestBody GovernanceAgentRequest request, HttpServletRequest httpRequest, Principal principal) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可生成 AI 治理计划");
        return agentService.plan(request, currentOperator(principal));
    }

    @PostMapping("/report")
    public Map<String, Object> report(@RequestBody GovernanceAgentRequest request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可生成 AI 治理报告");
        return agentService.report(request);
    }

    private String currentOperator(Principal principal) {
        return principal == null || principal.getName() == null ? "system" : principal.getName();
    }
}
