package com.dga.access.service;

import com.dga.access.dto.OffboardingRevocationRequest;
import com.dga.access.dto.OffboardingRevocationPreview;
import com.dga.access.dto.OffboardingRevocationResult;
import com.dga.access.dto.OffboardingRevocationTaskView;
import com.dga.access.entity.DgaUser;
import com.dga.access.entity.OffboardingRevocationTask;
import com.dga.access.repository.DgaUserRepository;
import com.dga.access.repository.OffboardingRevocationTaskRepository;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.settings.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OffboardingRevocationService {

    private static final List<String> PROTECTED_BIGDATA_USERS = Arrays.asList(
            "alading", "bf_hpt", "bf_hpt1", "md_bf", "hdfs", "hive", "yarn",
            "spark", "hbase", "impala", "sentry", "ranger"
    );

    private static final List<String> ACTIVE_TASK_STATUSES = Arrays.asList("PENDING", "RUNNING");

    @Autowired
    private DgaUserRepository dgaUserRepository;

    @Autowired
    private OffboardingRevocationTaskRepository taskRepository;

    @Autowired
    private UserResourceAccessRepository userResourceAccessRepository;

    @Autowired
    private LdapService ldapService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccessGovernanceService accessGovernanceService;

    public OffboardingRevocationResult execute(OffboardingRevocationRequest request) {
        return execute(request, "system");
    }

    public List<OffboardingRevocationTaskView> listTasks(String status, Integer limit) {
        int pageSize = normalizeLimit(limit);
        List<OffboardingRevocationTask> tasks;
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            tasks = taskRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, pageSize));
        } else {
            tasks = taskRepository.findByStatusOrderByCreatedAtDesc(
                    normalizedStatus.toUpperCase(Locale.ROOT), PageRequest.of(0, pageSize));
        }
        List<OffboardingRevocationTaskView> views = new ArrayList<>();
        for (OffboardingRevocationTask task : tasks) {
            views.add(toTaskView(task, false));
        }
        return views;
    }

    public OffboardingRevocationTaskView getTask(Long taskId) {
        if (taskId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择任务");
        }
        OffboardingRevocationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "离职权限回收任务不存在"));
        return toTaskView(task, true);
    }

    public OffboardingRevocationPreview preview(OffboardingRevocationRequest request) {
        String username = requireText(request == null ? null : request.getUsername(), "请选择离职用户");
        String cluster = requireText(request == null ? null : request.getCluster(), "请选择用户所属集群");
        LocalDate departureDate = request == null ? null : request.getDepartureDate();

        DgaUser user = resolveUser(username, cluster);
        String resolvedCluster = firstNonBlank(user == null ? null : user.getClusterName(), cluster);
        RevocationFlow flow = resolveRevocationFlow(resolvedCluster, user);

        OffboardingRevocationPreview preview = new OffboardingRevocationPreview();
        preview.setUsername(username);
        preview.setCluster(resolvedCluster);
        preview.setDepartureDate(departureDate);
        preview.setRevocationFlow(flow.label);
        preview.setEngineLabel(flow.engineLabel);
        preview.setNativeSqlFlow(flow.nativeSql);
        preview.setUserResolved(user != null);
        preview.setNotificationRecipients(buildRecipients(request));

        if (user == null) {
            preview.addWarning("DGA 本地用户记录不存在，将只能基于授权后端账号状态和确认单人工复核。");
        } else {
            preview.setDisplayName(user.getDisplayName());
            preview.setEmail(user.getEmail());
            preview.setCreationStrategy(user.getCreationStrategy());
            preview.setUserType(user.getUserType());
            preview.setLdapLocked(user.getLdapLocked());
        }

        if (departureDate == null) {
            preview.addBlocker("请选择离职日期。");
        }

        boolean protectedUser = isProtectedUser(user, username);
        preview.setProtectedUser(protectedUser);
        if (protectedUser) {
            preview.addBlocker("大数据保护用户不能执行离职权限回收。");
        }

        OffboardingRevocationTask activeTask = taskRepository
                .findFirstByUsernameAndClusterAndStatusInOrderByCreatedAtDesc(username, resolvedCluster, ACTIVE_TASK_STATUSES);
        if (activeTask != null) {
            preview.setActiveTaskExists(true);
            preview.setActiveTaskId(activeTask.getId());
            preview.setActiveTaskNo(activeTask.getTaskNo());
            preview.setActiveTaskStatus(activeTask.getStatus());
            preview.addBlocker("该用户已有待执行或执行中的离职权限回收任务: " + activeTask.getTaskNo());
        }

        if (preview.getNotificationRecipients().isEmpty()) {
            preview.addWarning("未填写通知对象，任务创建和回收完成通知需要人工补充。");
        }

        fillAccountPreview(preview, flow, username, resolvedCluster);
        fillPermissionPreview(preview, username, resolvedCluster);
        preview.setPlannedSteps(buildPlannedSteps(flow, departureDate, preview.getNotificationRecipients()));
        preview.setExecutable(preview.getBlockers().isEmpty());
        return preview;
    }

    public OffboardingRevocationResult execute(OffboardingRevocationRequest request, String operator) {
        String username = requireText(request == null ? null : request.getUsername(), "请选择离职用户");
        String cluster = requireText(request == null ? null : request.getCluster(), "请选择用户所属集群");
        LocalDate departureDate = request == null ? null : request.getDepartureDate();
        if (departureDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择离职日期");
        }

        DgaUser user = resolveUser(username, cluster);
        assertNotProtected(user, username);
        String resolvedCluster = firstNonBlank(user == null ? null : user.getClusterName(), cluster);
        if (taskRepository.existsByUsernameAndClusterAndStatusIn(username, resolvedCluster, ACTIVE_TASK_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该用户已有待执行或执行中的离职权限回收任务");
        }

        OffboardingRevocationTask task = createTask(request, username, resolvedCluster, operator);
        List<OffboardingRevocationResult.StepResult> creationSteps = createTaskReminderSteps(task);
        if (task.getScheduledAt().isAfter(LocalDateTime.now())) {
            return buildScheduledResult(task, creationSteps);
        }
        return executeRevocationTask(task, requestFromTask(task), creationSteps);
    }

    public void executeScheduledTask(Long taskId) {
        OffboardingRevocationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !"PENDING".equals(task.getStatus()) || task.getScheduledAt() == null
                || task.getScheduledAt().isAfter(LocalDateTime.now())) {
            return;
        }
        executeRevocationTask(task, requestFromTask(task), Collections.<OffboardingRevocationResult.StepResult>emptyList());
    }

    private OffboardingRevocationTask createTask(OffboardingRevocationRequest request, String username,
                                                 String cluster, String operator) {
        LocalDateTime now = LocalDateTime.now();
        OffboardingRevocationTask task = new OffboardingRevocationTask();
        task.setTaskNo(buildTaskNo(username, now));
        task.setUsername(username);
        task.setCluster(cluster);
        task.setDepartureDate(request.getDepartureDate());
        task.setScheduledAt(request.getDepartureDate().atStartOfDay());
        task.setManagerContact(trimToNull(request.getManagerContact()));
        task.setSecurityContact(trimToNull(request.getSecurityContact()));
        task.setReason(firstNonBlank(request.getReason(), "离职权限回收"));
        task.setStatus("PENDING");
        task.setCreatedBy(firstNonBlank(operator, "system"));
        task.setCreatedAt(now);
        task.setMessage("离职权限回收任务已创建，等待定时扫描执行");
        return taskRepository.save(task);
    }

    private List<OffboardingRevocationResult.StepResult> createTaskReminderSteps(OffboardingRevocationTask task) {
        List<OffboardingRevocationResult.StepResult> steps = new ArrayList<>();
        steps.add(new OffboardingRevocationResult.StepResult(
                "task-created",
                "创建离职权限回收任务",
                "SUCCESS",
                "任务已创建: " + task.getTaskNo() + "，计划执行时间: " + task.getScheduledAt()
        ));

        List<String> recipients = buildRecipients(task);
        if (recipients.isEmpty()) {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "notify-created",
                    "发送任务创建提醒",
                    "PENDING",
                    "未填写通知对象，任务已创建待人工通知"
            ));
            return steps;
        }
        try {
            String message = notificationService.notifyOffboardingTaskCreated(
                    task.getUsername(), task.getCluster(), task.getDepartureDate(), recipients, task.getTaskNo());
            steps.add(new OffboardingRevocationResult.StepResult("notify-created", "发送任务创建提醒", "SUCCESS", message));
        } catch (Exception e) {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "notify-created",
                    "发送任务创建提醒",
                    "PENDING",
                    "任务已创建，但创建提醒未完成；" + readableMessage(e)
            ));
        }
        return steps;
    }

    private OffboardingRevocationResult buildScheduledResult(OffboardingRevocationTask task,
                                                             List<OffboardingRevocationResult.StepResult> steps) {
        OffboardingRevocationResult result = baseResult(task);
        RevocationFlow flow = resolveRevocationFlow(task.getCluster(), resolveUser(task.getUsername(), task.getCluster()));
        result.setRevocationFlow(flow.label);
        result.setConfirmationNo(task.getTaskNo());
        result.setNotificationRecipients(buildRecipients(task));
        for (OffboardingRevocationResult.StepResult step : steps) {
            result.addStep(step);
        }
        result.addStep(new OffboardingRevocationResult.StepResult(
                "scheduled-wait",
                "等待离职日期到达",
                "PENDING",
                "当前时间未到离职日期，定时任务将在 " + task.getScheduledAt() + " 后自动扫描执行"
        ));
        addWaitingExecutionSteps(result, flow);
        result.setConfirmationText(buildScheduledTaskText(result, task));
        return result;
    }

    private OffboardingRevocationResult executeRevocationTask(OffboardingRevocationTask task,
                                                              OffboardingRevocationRequest request,
                                                              List<OffboardingRevocationResult.StepResult> preSteps) {
        if (!"PENDING".equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "任务当前状态不能执行: " + task.getStatus());
        }

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setMessage("离职权限回收执行中");
        taskRepository.save(task);

        String username = task.getUsername();
        String cluster = task.getCluster();
        LocalDate departureDate = task.getDepartureDate();
        DgaUser user = resolveUser(username, cluster);
        RevocationFlow flow = resolveRevocationFlow(cluster, user);

        OffboardingRevocationResult result = baseResult(task);
        result.setRevocationFlow(flow.label);
        result.setExecutedAt(task.getStartedAt());
        result.setConfirmationNo(buildConfirmationNo(username, task.getStartedAt()));
        result.setNotificationRecipients(buildRecipients(task));
        for (OffboardingRevocationResult.StepResult step : preSteps) {
            result.addStep(step);
        }

        if (flow.nativeSql) {
            executeNativeSqlFlow(result, user, username, cluster, departureDate, flow);
        } else {
            executeLdapBigdataFlow(result, user, username, cluster, departureDate);
        }

        result.addStep(new OffboardingRevocationResult.StepResult(
                "cloud-accesskey-delete",
                "云账号 AccessKey 删除",
                "PENDING",
                "当前系统未配置云账号 AccessKey 删除脚本，已纳入确认单待安全团队核验"
        ));

        runStep(result, "residual-account-governance", "离职后账号残留风险登记", new StepAction() {
            @Override
            public String run() {
                return registerResidualAccountRisk(task, result, flow);
            }
        });

        List<String> recipients = buildRecipients(task);
        runStep(result, "notify", "发送回收完成通知", new StepAction() {
            @Override
            public String run() {
                if (recipients.isEmpty()) {
                    return pending("未填写通知对象，已生成确认单待人工通知");
                }
                try {
                    return notificationService.notifyOffboardingRevocation(username, result.getCluster(),
                            departureDate, recipients, result.getConfirmationNo());
                } catch (Exception e) {
                    return pending("当前未完成回收完成通知；" + readableMessage(e)
                            + "。通知对象已生成: " + String.join("，", recipients));
                }
            }
        });

        result.setArchiveName(result.getConfirmationNo() + "-权限回收确认单.txt");
        result.addStep(new OffboardingRevocationResult.StepResult(
                "archive",
                "生成《权限回收确认单》归档",
                "SUCCESS",
                "已生成确认单: " + result.getArchiveName()
        ));
        result.setConfirmationText(buildConfirmationText(result, request));

        boolean cleanupFailed = hasFailedCleanupStep(result);
        task.setStatus(cleanupFailed ? "FAILED" : "COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setConfirmationNo(result.getConfirmationNo());
        task.setArchiveName(result.getArchiveName());
        task.setConfirmationText(result.getConfirmationText());
        task.setMessage(cleanupFailed ? "离职权限回收存在失败步骤，请查看确认单" : "离职权限回收已执行完成");
        taskRepository.save(task);
        result.setTaskStatus(task.getStatus());
        return result;
    }

    private OffboardingRevocationResult baseResult(OffboardingRevocationTask task) {
        OffboardingRevocationResult result = new OffboardingRevocationResult();
        result.setTaskId(task.getId());
        result.setTaskNo(task.getTaskNo());
        result.setTaskStatus(task.getStatus());
        result.setUsername(task.getUsername());
        result.setCluster(task.getCluster());
        result.setDepartureDate(task.getDepartureDate());
        result.setScheduledAt(task.getScheduledAt());
        result.setRevocationFlow(resolveRevocationFlow(task.getCluster(), null).label);
        return result;
    }

    private OffboardingRevocationTaskView toTaskView(OffboardingRevocationTask task, boolean includeConfirmationText) {
        OffboardingRevocationTaskView view = new OffboardingRevocationTaskView();
        view.setTaskId(task.getId());
        view.setTaskNo(task.getTaskNo());
        view.setUsername(task.getUsername());
        view.setCluster(task.getCluster());
        view.setDepartureDate(task.getDepartureDate());
        view.setScheduledAt(task.getScheduledAt());
        view.setManagerContact(task.getManagerContact());
        view.setSecurityContact(task.getSecurityContact());
        view.setReason(task.getReason());
        view.setStatus(task.getStatus());
        view.setCreatedBy(task.getCreatedBy());
        view.setCreatedAt(task.getCreatedAt());
        view.setStartedAt(task.getStartedAt());
        view.setCompletedAt(task.getCompletedAt());
        view.setConfirmationNo(task.getConfirmationNo());
        view.setArchiveName(task.getArchiveName());
        view.setMessage(task.getMessage());
        if (includeConfirmationText) {
            view.setConfirmationText(task.getConfirmationText());
        }
        return view;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private void fillAccountPreview(OffboardingRevocationPreview preview, RevocationFlow flow,
                                    String username, String cluster) {
        try {
            boolean exists = flow.nativeSql
                    ? authorizationService.userExists(cluster, username, null)
                    : ldapService.userExists(cluster, username);
            preview.setAccountExists(exists);
            preview.setAccountCheckStatus(exists ? "FOUND" : "MISSING");
            preview.setAccountCheckMessage(exists
                    ? "已确认 " + flow.engineLabel + " 账号存在"
                    : flow.engineLabel + " 账号当前不存在，提交后将继续做本地状态标记和确认单归档");
            if (!exists) {
                preview.addWarning(preview.getAccountCheckMessage());
            }
        } catch (Exception e) {
            preview.setAccountExists(null);
            preview.setAccountCheckStatus("FAILED");
            preview.setAccountCheckMessage("账号状态预检失败：" + readableMessage(e));
            preview.addWarning(preview.getAccountCheckMessage());
        }
    }

    private void fillPermissionPreview(OffboardingRevocationPreview preview, String username, String cluster) {
        try {
            List<Map<String, Object>> livePermissions = authorizationService.getUserPermissions(username, cluster);
            int liveCount = livePermissions == null ? 0 : livePermissions.size();
            preview.setLivePermissionCount(liveCount);
            preview.setLivePermissionMessage("实时授权后端可见权限 " + liveCount + " 条");
            if (liveCount == 0) {
                preview.addWarning("实时授权后端未返回权限，请结合本地台账和回收后复核确认。");
            }
        } catch (Exception e) {
            preview.setLivePermissionCount(null);
            preview.setLivePermissionMessage("实时权限预检失败：" + readableMessage(e));
            preview.addWarning(preview.getLivePermissionMessage());
        }

        try {
            preview.setRecordedPermissionCount(userResourceAccessRepository.countActiveByUsernameAndCluster(username, cluster));
        } catch (Exception e) {
            preview.addWarning("本地权限台账统计失败：" + readableMessage(e));
        }
    }

    private List<OffboardingRevocationResult.StepResult> buildPlannedSteps(RevocationFlow flow, LocalDate departureDate,
                                                                            List<String> recipients) {
        List<OffboardingRevocationResult.StepResult> steps = new ArrayList<>();
        steps.add(new OffboardingRevocationResult.StepResult(
                "task-created", "创建离职权限回收任务", "WAIT", "提交后创建任务并记录操作人"));
        steps.add(new OffboardingRevocationResult.StepResult(
                "notify-created", "发送任务创建提醒",
                recipients == null || recipients.isEmpty() ? "PENDING" : "WAIT",
                recipients == null || recipients.isEmpty() ? "未填写通知对象，需人工补充" : "提交后通知直属主管和安全团队"));
        if (departureDate != null && departureDate.atStartOfDay().isAfter(LocalDateTime.now())) {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "scheduled-wait", "等待离职日期到达", "PENDING",
                    "定时任务将在 " + departureDate.atStartOfDay() + " 后自动扫描执行"));
        } else {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "scheduled-wait", "离职日期校验", "WAIT", "提交后立即进入回收执行"));
        }

        if (flow.nativeSql) {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "native-user-check", flow.engineLabel + " 原生账号校验", "WAIT", "执行前确认原生账号是否存在"));
            steps.add(new OffboardingRevocationResult.StepResult(
                    "native-account-drop", flow.engineLabel + " 原生账号删除与权限撤销", "WAIT", "通过授权后端删除原生用户"));
            steps.add(new OffboardingRevocationResult.StepResult(
                    "local-user-expire", "DGA 本地用户状态标记", "WAIT", "标记用户到期时间"));
        } else {
            steps.add(new OffboardingRevocationResult.StepResult(
                    "ldap-disable", "OpenLDAP 账户禁用", "WAIT", "禁用 LDAP 登录能力"));
            steps.add(new OffboardingRevocationResult.StepResult(
                    "group-remove", "用户组移除", "WAIT", "清空 LDAP 附加组，主组保留用于 POSIX 一致性"));
            steps.add(new OffboardingRevocationResult.StepResult(
                    "bigdata-revoke", "大数据平台权限撤销", "WAIT", "调用当前集群授权后端执行回收"));
        }
        steps.add(new OffboardingRevocationResult.StepResult(
                "cloud-accesskey-delete", "云账号 AccessKey 删除", "PENDING", "当前系统未配置自动删除脚本，需安全团队核验"));
        steps.add(new OffboardingRevocationResult.StepResult(
                "residual-account-governance", "离职后账号残留风险登记", "WAIT", "回收后复核，异常写入风险治理"));
        steps.add(new OffboardingRevocationResult.StepResult(
                "notify", "发送回收完成通知", "WAIT", "回收完成后发送确认通知"));
        steps.add(new OffboardingRevocationResult.StepResult(
                "archive", "生成《权限回收确认单》归档", "WAIT", "生成归档文本"));
        return steps;
    }

    private void executeLdapBigdataFlow(OffboardingRevocationResult result, DgaUser user, String username,
                                        String cluster, LocalDate departureDate) {
        runStep(result, "ldap-disable", "OpenLDAP 账户禁用", new StepAction() {
            @Override
            public String run() {
                java.util.Map<String, Object> disableResult = ldapService.disableUserForOffboarding(cluster, username);
                if (user != null) {
                    user.setLdapLocked(true);
                    user.setExpiresAt(departureDate.atStartOfDay());
                    dgaUserRepository.save(user);
                }
                Object message = disableResult.get("message");
                return message == null ? "已禁用 LDAP 账户" : String.valueOf(message);
            }
        });

        runStep(result, "group-remove", "用户组移除", new StepAction() {
            @Override
            public String run() {
                ldapService.updateUserSupplementaryGroups(cluster, username, Collections.<String>emptyList());
                if (user != null) {
                    user.setSupplementaryGroups("[]");
                    dgaUserRepository.save(user);
                }
                return "已清空 LDAP 附加组；主组保留用于 POSIX 账号一致性";
            }
        });

        runStep(result, "bigdata-revoke", "大数据平台权限撤销", new StepAction() {
            @Override
            public String run() {
                authorizationService.revokeAll(username, cluster);
                return "已调用当前集群授权后端执行全量权限回收";
            }
        });
    }

    private void executeNativeSqlFlow(OffboardingRevocationResult result, DgaUser user, String username,
                                      String cluster, LocalDate departureDate, RevocationFlow flow) {
        runStep(result, "native-user-check", flow.engineLabel + " 原生账号校验", new StepAction() {
            @Override
            public String run() {
                boolean exists = authorizationService.userExists(cluster, username, null);
                return exists ? "已定位 " + flow.engineLabel + " 原生账号" : pending(flow.engineLabel + " 原生账号当前不存在，继续执行本地状态标记和确认单归档");
            }
        });

        runStep(result, "native-account-drop", flow.engineLabel + " 原生账号删除与权限撤销", new StepAction() {
            @Override
            public String run() {
                authorizationService.revokeAll(username, cluster);
                return "已完成 " + flow.engineLabel + " 原生用户删除或不存在确认，直连授权和角色绑定随账号完成回收";
            }
        });

        runStep(result, "local-user-expire", "DGA 本地用户状态标记", new StepAction() {
            @Override
            public String run() {
                if (user == null) {
                    return pending("DGA 本地用户记录不存在，已完成原生账号回收；请在确认单中人工核验本地台账");
                }
                user.setExpiresAt(departureDate.atStartOfDay());
                dgaUserRepository.save(user);
                return "已将 DGA 本地用户到期时间标记为 " + departureDate.atStartOfDay();
            }
        });
    }

    private OffboardingRevocationRequest requestFromTask(OffboardingRevocationTask task) {
        OffboardingRevocationRequest request = new OffboardingRevocationRequest();
        request.setUsername(task.getUsername());
        request.setCluster(task.getCluster());
        request.setDepartureDate(task.getDepartureDate());
        request.setManagerContact(task.getManagerContact());
        request.setSecurityContact(task.getSecurityContact());
        request.setReason(task.getReason());
        return request;
    }

    private DgaUser resolveUser(String username, String cluster) {
        DgaUser user = dgaUserRepository.findByUsernameAndClusterNameAndIsDeletedFalse(username, cluster);
        if (user != null) {
            return user;
        }
        List<DgaUser> users = dgaUserRepository.findByUsernameAndIsDeletedFalse(username);
        if (users == null || users.isEmpty()) {
            return null;
        }
        for (DgaUser item : users) {
            if (equalsIgnoreCase(item.getClusterName(), cluster)) {
                return item;
            }
        }
        return users.size() == 1 ? users.get(0) : null;
    }

    private void assertNotProtected(DgaUser user, String username) {
        if (isProtectedUser(user, username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "大数据保护用户不能执行离职权限回收");
        }
    }

    private boolean isProtectedUser(DgaUser user, String username) {
        String normalized = username == null ? "" : username.toLowerCase(Locale.ROOT);
        return PROTECTED_BIGDATA_USERS.contains(normalized)
                || (user != null && Boolean.TRUE.equals(user.getProtectedUser()));
    }

    private void runStep(OffboardingRevocationResult result, String key, String title, StepAction action) {
        try {
            String message = action.run();
            String status = "SUCCESS";
            if (message != null && message.startsWith("__PENDING__")) {
                status = "PENDING";
                message = message.substring("__PENDING__".length());
            }
            result.addStep(new OffboardingRevocationResult.StepResult(key, title, status, message));
        } catch (Exception e) {
            result.addStep(new OffboardingRevocationResult.StepResult(key, title, "FAILED", readableMessage(e)));
        }
    }

    private String pending(String message) {
        return "__PENDING__" + message;
    }

    private List<String> buildRecipients(OffboardingRevocationRequest request) {
        List<String> recipients = new ArrayList<>();
        if (request == null) {
            return recipients;
        }
        addIfPresent(recipients, request.getManagerContact());
        addIfPresent(recipients, request.getSecurityContact());
        return recipients;
    }

    private List<String> buildRecipients(OffboardingRevocationTask task) {
        List<String> recipients = new ArrayList<>();
        addIfPresent(recipients, task.getManagerContact());
        addIfPresent(recipients, task.getSecurityContact());
        return recipients;
    }

    private void addWaitingExecutionSteps(OffboardingRevocationResult result, RevocationFlow flow) {
        if (flow.nativeSql) {
            result.addStep(new OffboardingRevocationResult.StepResult("native-user-check", flow.engineLabel + " 原生账号校验", "PENDING", "等待到期执行"));
            result.addStep(new OffboardingRevocationResult.StepResult("native-account-drop", flow.engineLabel + " 原生账号删除与权限撤销", "PENDING", "等待到期执行"));
            result.addStep(new OffboardingRevocationResult.StepResult("local-user-expire", "DGA 本地用户状态标记", "PENDING", "等待到期执行"));
        } else {
            result.addStep(new OffboardingRevocationResult.StepResult("ldap-disable", "OpenLDAP 账户禁用", "PENDING", "等待到期执行"));
            result.addStep(new OffboardingRevocationResult.StepResult("group-remove", "用户组移除", "PENDING", "等待到期执行"));
            result.addStep(new OffboardingRevocationResult.StepResult("bigdata-revoke", "大数据平台权限撤销", "PENDING", "等待到期执行"));
        }
        result.addStep(new OffboardingRevocationResult.StepResult("cloud-accesskey-delete", "云账号 AccessKey 删除", "PENDING", "等待到期执行"));
        result.addStep(new OffboardingRevocationResult.StepResult("residual-account-governance", "离职后账号残留风险登记", "PENDING", "等待回收完成后复核"));
        result.addStep(new OffboardingRevocationResult.StepResult("notify", "发送回收完成通知", "PENDING", "等待权限回收完成后发送"));
        result.addStep(new OffboardingRevocationResult.StepResult("archive", "生成《权限回收确认单》归档", "PENDING", "等待权限回收完成后归档"));
    }

    private boolean hasFailedCleanupStep(OffboardingRevocationResult result) {
        for (OffboardingRevocationResult.StepResult step : result.getSteps()) {
            if ("FAILED".equals(step.getStatus())
                    && !"notify".equals(step.getKey())
                    && !"notify-created".equals(step.getKey())
                    && !"residual-account-governance".equals(step.getKey())
                    && !"archive".equals(step.getKey())) {
                return true;
            }
        }
        return false;
    }

    private String buildTaskNo(String username, LocalDateTime now) {
        String suffix = username == null ? "UNKNOWN" : username.replaceAll("[^A-Za-z0-9_-]", "_");
        return "OFFBOARD-TASK-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + suffix;
    }

    private String buildConfirmationNo(String username, LocalDateTime now) {
        String suffix = username == null ? "UNKNOWN" : username.replaceAll("[^A-Za-z0-9_-]", "_");
        return "OFFBOARD-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + suffix;
    }

    private String buildScheduledTaskText(OffboardingRevocationResult result, OffboardingRevocationTask task) {
        StringBuilder builder = new StringBuilder();
        builder.append("《离职权限回收任务单》\n");
        builder.append("任务编号: ").append(task.getTaskNo()).append('\n');
        builder.append("用户: ").append(task.getUsername()).append('\n');
        builder.append("集群: ").append(task.getCluster()).append('\n');
        builder.append("回收流程: ").append(result.getRevocationFlow()).append('\n');
        builder.append("离职日期: ").append(task.getDepartureDate()).append('\n');
        builder.append("计划执行时间: ").append(task.getScheduledAt()).append('\n');
        builder.append("创建时间: ").append(task.getCreatedAt()).append('\n');
        builder.append("任务状态: ").append(task.getStatus()).append('\n');
        if (trimToNull(task.getReason()) != null) {
            builder.append("回收原因: ").append(task.getReason().trim()).append('\n');
        }
        builder.append("通知对象: ")
                .append(result.getNotificationRecipients().isEmpty() ? "待补充" : String.join("，", result.getNotificationRecipients()))
                .append('\n');
        builder.append("任务步骤:\n");
        for (OffboardingRevocationResult.StepResult step : result.getSteps()) {
            builder.append("- [").append(step.getStatus()).append("] ")
                    .append(step.getTitle()).append(": ")
                    .append(step.getMessage()).append('\n');
        }
        return builder.toString();
    }

    private String buildConfirmationText(OffboardingRevocationResult result, OffboardingRevocationRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("《权限回收确认单》\n");
        builder.append("任务编号: ").append(result.getTaskNo()).append('\n');
        builder.append("确认单编号: ").append(result.getConfirmationNo()).append('\n');
        builder.append("用户: ").append(result.getUsername()).append('\n');
        builder.append("集群: ").append(result.getCluster()).append('\n');
        builder.append("回收流程: ").append(result.getRevocationFlow()).append('\n');
        builder.append("离职日期: ").append(result.getDepartureDate()).append('\n');
        builder.append("计划执行时间: ").append(result.getScheduledAt()).append('\n');
        builder.append("执行时间: ").append(result.getExecutedAt()).append('\n');
        if (trimToNull(request.getReason()) != null) {
            builder.append("回收原因: ").append(request.getReason().trim()).append('\n');
        }
        builder.append("通知对象: ")
                .append(result.getNotificationRecipients().isEmpty() ? "待补充" : String.join("，", result.getNotificationRecipients()))
                .append('\n');
        builder.append("回收步骤:\n");
        for (OffboardingRevocationResult.StepResult step : result.getSteps()) {
            builder.append("- [").append(step.getStatus()).append("] ")
                    .append(step.getTitle()).append(": ")
                    .append(step.getMessage()).append('\n');
        }
        return builder.toString();
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void addIfPresent(List<String> values, String value) {
        String normalized = trimToNull(value);
        if (normalized != null && !values.contains(normalized)) {
            values.add(normalized);
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private RevocationFlow resolveRevocationFlow(String cluster, DgaUser user) {
        String engine = null;
        try {
            engine = authorizationService.engineType(cluster);
        } catch (Exception ignored) {
            // Cluster capability discovery may be unavailable for imported users; fall back to user source.
        }
        String source = firstNonBlank(engine, user == null ? null : user.getCreationStrategy());
        String normalized = source == null ? "" : source.toUpperCase(Locale.ROOT);
        if (normalized.contains("DORIS")) {
            return new RevocationFlow(true, "Doris", "Doris 原生账号回收");
        }
        if (normalized.contains("STARROCKS") || "SR".equals(normalized)) {
            return new RevocationFlow(true, "StarRocks", "StarRocks 原生账号回收");
        }
        return new RevocationFlow(false, "LDAP", "LDAP + 大数据平台权限回收");
    }

    private String registerResidualAccountRisk(OffboardingRevocationTask task,
                                               OffboardingRevocationResult result,
                                               RevocationFlow flow) {
        Boolean accountExists = null;
        String evidence = null;
        try {
            accountExists = flow.nativeSql
                    ? authorizationService.userExists(task.getCluster(), task.getUsername(), null)
                    : ldapService.userExists(task.getCluster(), task.getUsername());
        } catch (Exception e) {
            evidence = "回收后账号状态复核失败：" + readableMessage(e);
        }

        boolean cleanupFailed = hasFailedCleanupStep(result);
        if (Boolean.TRUE.equals(accountExists) || cleanupFailed || accountExists == null) {
            String detail;
            if (Boolean.TRUE.equals(accountExists)) {
                detail = "回收后复核结果：账号仍存在。";
            } else if (cleanupFailed) {
                detail = "回收步骤存在失败，账号删除未被确认。";
            } else {
                detail = evidence == null ? "回收后账号状态未能确认。" : evidence;
            }
            accessGovernanceService.recordOffboardedAccountRemains(
                    task.getUsername(),
                    task.getCluster(),
                    flow.label,
                    task.getDepartureDate() == null ? null : task.getDepartureDate().atStartOfDay(),
                    result.getConfirmationNo(),
                    task.getCreatedBy(),
                    detail
            );
            return pending(detail + "已写入风险治理，待安全团队复核。");
        }
        return "回收后复核未发现残留账号";
    }

    private String readableMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "执行失败";
        }
        String message = e.getMessage();
        int nestedIndex = message.indexOf("; nested exception");
        if (nestedIndex > 0) {
            message = message.substring(0, nestedIndex);
        }
        return message.length() > 220 ? message.substring(0, 220) + "..." : message;
    }

    private interface StepAction {
        String run();
    }

    private static class RevocationFlow {
        private final boolean nativeSql;
        private final String engineLabel;
        private final String label;

        private RevocationFlow(boolean nativeSql, String engineLabel, String label) {
            this.nativeSql = nativeSql;
            this.engineLabel = engineLabel;
            this.label = label;
        }
    }
}
