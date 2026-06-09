package com.dga.access.controller;

import com.dga.access.dto.OffboardingRevocationRequest;
import com.dga.access.dto.OffboardingRevocationPreview;
import com.dga.access.dto.OffboardingRevocationResult;
import com.dga.access.dto.OffboardingRevocationTaskView;
import com.dga.access.security.CurrentUser;
import com.dga.access.service.AdminGuard;
import com.dga.access.service.OffboardingRevocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/access/offboarding")
@Tag(name = "离职权限回收", description = "离职用户账号禁用、组移除、权限回收、通知与确认单生成")
public class OffboardingRevocationController {

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private OffboardingRevocationService offboardingRevocationService;

    @GetMapping("/revocations")
    @Operation(summary = "查询离职权限回收任务", description = "按创建时间倒序返回最近的离职权限回收任务，可按状态过滤。")
    public List<OffboardingRevocationTaskView> list(@RequestParam(value = "status", required = false) String status,
                                                    @RequestParam(value = "limit", required = false) Integer limit,
                                                    HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可查看离职权限回收任务");
        return offboardingRevocationService.listTasks(status, limit);
    }

    @GetMapping("/revocations/{taskId}")
    @Operation(summary = "查询离职权限回收任务详情", description = "返回任务详情、确认单和归档信息。")
    public OffboardingRevocationTaskView detail(@PathVariable Long taskId,
                                                HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可查看离职权限回收任务");
        return offboardingRevocationService.getTask(taskId);
    }

    @PostMapping("/revocations/preview")
    @Operation(summary = "预检离职权限回收影响范围", description = "创建任务前检查保护用户、重复任务、账号状态、权限数量和计划执行步骤。")
    public OffboardingRevocationPreview preview(@RequestBody OffboardingRevocationRequest request,
                                                HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可预检离职权限回收");
        return offboardingRevocationService.preview(request);
    }

    @PostMapping("/revocations")
    @Operation(summary = "创建离职权限回收任务", description = "传入用户名、集群和离职日期；未来日期先创建定时任务，到期后自动执行并发送完成通知。")
    public OffboardingRevocationResult execute(@RequestBody OffboardingRevocationRequest request,
                                               HttpServletRequest servletRequest) {
        adminGuard.requirePlatformAdmin(servletRequest, "仅 admin 或超级用户可执行离职权限回收");
        return offboardingRevocationService.execute(request, CurrentUser.usernameOrUnknown());
    }
}
