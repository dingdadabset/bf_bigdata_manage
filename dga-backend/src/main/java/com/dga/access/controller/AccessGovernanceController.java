package com.dga.access.controller;

import com.dga.access.dto.AccessOwnerRequest;
import com.dga.access.dto.GovernanceIssueActionRequest;
import com.dga.access.entity.AccessGovernanceIssue;
import com.dga.access.entity.AccessOwner;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.security.CurrentUser;
import com.dga.access.service.AccessGovernanceService;
import com.dga.access.service.AdminGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access/governance")
@CrossOrigin
public class AccessGovernanceController {

    @Autowired
    private AccessGovernanceService governanceService;

    @Autowired
    private AdminGuard adminGuard;

    @GetMapping("/sources")
    public List<Map<String, Object>> sources(@RequestParam(required = false) String cluster,
                                             @RequestParam(required = false) String selectedSources) {
        return governanceService.sourceStatuses(cluster,
                governanceService.normalizeSourcesForApi(selectedSources));
    }

    @GetMapping("/owners")
    public List<Map<String, Object>> owners(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String cluster) {
        return governanceService.searchOwners(q, cluster);
    }

    @PostMapping("/owners")
    public AccessOwner createOwner(@RequestBody AccessOwnerRequest body, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可创建权限负责人");
        return governanceService.createOwner(body, CurrentUser.usernameOrUnknown());
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) String cluster) {
        return governanceService.summary(cluster);
    }

    @GetMapping("/issues")
    public Page<AccessGovernanceIssue> issues(@RequestParam(required = false) String cluster,
                                              @RequestParam(required = false) String issueType,
                                              @RequestParam(required = false) String issueTypes,
                                              @RequestParam(defaultValue = "OPEN") String status,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "severity", "detectedAt", "id"));
        String types = issueTypes != null && !issueTypes.trim().isEmpty() ? issueTypes : issueType;
        return governanceService.listIssues(cluster, types, status, username, pageable);
    }

    @PostMapping("/scan")
    public Map<String, Object> scan(@RequestParam(required = false) String cluster,
                                    @RequestParam(defaultValue = "90") int inactiveDays,
                                    @RequestParam(defaultValue = "90") int reviewDays,
                                    @RequestParam(required = false) String sources,
                                    HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可扫描权限治理风险");
        return governanceService.scan(cluster, inactiveDays, reviewDays, sources);
    }

    @DeleteMapping("/results")
    public Map<String, Object> clearResults(@RequestParam(required = false) String cluster,
                                            @RequestParam(defaultValue = "true") boolean includeEvidence,
                                            HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可清理权限治理检测结果");
        return governanceService.clearResults(cluster, includeEvidence, CurrentUser.usernameOrUnknown());
    }

    @PutMapping("/issues/{id}/claim")
    public AccessGovernanceIssue claim(@PathVariable Long id,
                                       @RequestBody(required = false) GovernanceIssueActionRequest body,
                                       HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可认领权限治理问题");
        String operator = CurrentUser.usernameOrUnknown();
        String owner = body == null ? null : body.getOwner();
        String assignee = body == null ? null : body.getAssignee();
        return governanceService.claimIssue(id, owner, body == null ? null : body.getCollaboratorOwners(),
                assignee, operator);
    }

    @PutMapping("/issues/{id}/resolve")
    public AccessGovernanceIssue resolve(@PathVariable Long id, HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可关闭权限治理问题");
        return governanceService.resolveIssue(id, CurrentUser.usernameOrUnknown());
    }

    @PutMapping("/issues/{id}/review")
    public AccessGovernanceIssue reviewIssue(@PathVariable Long id,
                                             @RequestBody(required = false) GovernanceIssueActionRequest body,
                                             HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可复核高权限");
        int reviewDays = body != null && body.getReviewDays() != null ? body.getReviewDays() : 90;
        return governanceService.markAccountReviewed(id, reviewDays, CurrentUser.usernameOrUnknown());
    }

    @PutMapping("/permissions/{accessId}/owner")
    public UserResourceAccess setPermissionOwner(@PathVariable Long accessId,
                                                 @RequestBody GovernanceIssueActionRequest body,
                                                 HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可设置权限 owner");
        String owner = body == null ? null : body.getOwner();
        return governanceService.setPermissionOwner(accessId, owner,
                body == null ? null : body.getCollaboratorOwners(),
                body == null ? null : body.getOwnerDisplayName(),
                body == null ? null : body.getOwnerEmail(),
                CurrentUser.usernameOrUnknown());
    }

    @PutMapping("/permissions/{accessId}/review")
    public UserResourceAccess reviewPermission(@PathVariable Long accessId,
                                               @RequestBody(required = false) GovernanceIssueActionRequest body,
                                               HttpServletRequest request) {
        adminGuard.requirePlatformAdmin(request, "仅 admin 或超级用户可复核高权限");
        int reviewDays = body != null && body.getReviewDays() != null ? body.getReviewDays() : 90;
        return governanceService.markPermissionReviewed(accessId, reviewDays, CurrentUser.usernameOrUnknown());
    }
}
