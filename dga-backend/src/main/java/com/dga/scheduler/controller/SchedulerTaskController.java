package com.dga.scheduler.controller;

import com.dga.access.service.AdminGuard;
import com.dga.scheduler.entity.SchedulerTaskOwner;
import com.dga.scheduler.service.SchedulerTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerTaskController {

    @Autowired
    private SchedulerTaskService schedulerTaskService;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(required = false) String clusterCode,
                                        @RequestParam(required = false) Long endpointId) {
        return schedulerTaskService.overview(clusterCode, endpointId);
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> tasks(@RequestParam(required = false) String clusterCode,
                                           @RequestParam(required = false) Long endpointId,
                                           @RequestParam(required = false) String projectName,
                                           @RequestParam(required = false) String flowName,
                                           @RequestParam(required = false) String owner,
                                           @RequestParam(required = false) String health,
                                           @RequestParam(required = false) Boolean assetActive,
                                           @RequestParam(required = false) String keyword) {
        return schedulerTaskService.tasks(clusterCode, endpointId, projectName, flowName, owner, health, assetActive, keyword);
    }

    @GetMapping("/risk-tasks")
    public List<Map<String, Object>> riskTasks(@RequestParam(required = false) String clusterCode,
                                               @RequestParam(required = false) Long endpointId,
                                               @RequestParam(required = false) String day) {
        return schedulerTaskService.riskTasks(clusterCode, endpointId, day);
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> groups(@RequestParam(required = false) String clusterCode,
                                            @RequestParam(required = false) Long endpointId) {
        return schedulerTaskService.groups(clusterCode, endpointId);
    }

    @PutMapping("/tasks/owner")
    public SchedulerTaskOwner assignOwner(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        adminGuard.requirePlatformAdmin(httpRequest, "仅 admin 或超级用户可维护调度任务责任人");
        return schedulerTaskService.assignOwner(request);
    }
}
