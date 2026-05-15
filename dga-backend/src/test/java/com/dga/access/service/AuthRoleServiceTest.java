package com.dga.access.service;

import com.dga.access.dto.BatchRoleAssignmentItem;
import com.dga.access.dto.BatchRoleAssignmentRequest;
import com.dga.access.dto.BatchRoleAssignmentResult;
import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthUserRole;
import com.dga.access.repository.AuthRoleAssignmentAuditRepository;
import com.dga.access.repository.AuthRolePermissionRepository;
import com.dga.access.repository.AuthRoleRepository;
import com.dga.access.repository.AuthUserRoleRepository;
import com.dga.access.service.authorization.AuthorizationCapability;
import com.dga.access.service.authorization.AuthorizationService;
import com.dga.cluster.entity.ClusterEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthRoleServiceTest {

    @Mock
    private AuthRoleRepository roleRepository;

    @Mock
    private AuthRolePermissionRepository permissionRepository;

    @Mock
    private AuthUserRoleRepository userRoleRepository;

    @Mock
    private AuthRoleAssignmentAuditRepository auditRepository;

    @Mock
    private AuthorizationService authorizationService;

    private AuthRoleService service;

    @BeforeEach
    void setUp() {
        service = new AuthRoleService();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "userRoleRepository", userRoleRepository);
        ReflectionTestUtils.setField(service, "auditRepository", auditRepository);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);

        when(authorizationService.normalizeAuthBackend(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authorizationService.authBackend(anyString())).thenReturn(ClusterEndpoint.AUTH_SENTRY);
        when(authorizationService.capability(anyString(), anyString())).thenAnswer(invocation -> capability(invocation.getArgument(1)));
    }

    @Test
    void dryRunBatchAssignmentsRejectsMissingUsers() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));

        BatchRoleAssignmentRequest request = request(ClusterEndpoint.AUTH_SENTRY);
        request.setUsernames(Collections.emptyList());

        assertThatThrownBy(() -> service.dryRunBatchAssignments("dga_role", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("请提供用户列表");
    }

    @Test
    void dryRunBatchAssignmentsRejectsClusterMismatch() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));

        BatchRoleAssignmentRequest request = request(ClusterEndpoint.AUTH_SENTRY, "user1");
        request.setCluster("HDP");

        assertThatThrownBy(() -> service.dryRunBatchAssignments("dga_role", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("角色不属于当前集群");
    }

    @Test
    void dryRunBatchAssignmentsRejectsMoreThanFiveHundredUsers() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));

        BatchRoleAssignmentRequest request = request(ClusterEndpoint.AUTH_SENTRY);
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            users.add("user" + i);
        }
        request.setUsernames(users);

        assertThatThrownBy(() -> service.dryRunBatchAssignments("dga_role", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("单次最多支持 500 个用户");
    }

    @Test
    void dryRunBatchAssignmentsClassifiesBlankDuplicateAlreadyBoundAndWillBindUsers() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.singletonList(existingAssignment("existing", ClusterEndpoint.AUTH_SENTRY, "SUCCESS")));

        BatchRoleAssignmentRequest request = request(ClusterEndpoint.AUTH_SENTRY,
                " ", "Existing", "existing", "new_user");

        BatchRoleAssignmentResult result = service.dryRunBatchAssignments("dga_role", request);

        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getValidCount()).isEqualTo(2);
        assertThat(result.getDuplicateCount()).isEqualTo(1);
        assertThat(result.getAlreadyBoundCount()).isEqualTo(1);
        assertThat(result.getWillBindCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getPendingGroupMappingCount()).isEqualTo(1);

        assertItem(result, 0, "INVALID", "FAILED");
        assertItem(result, 1, "ALREADY_BOUND", "SUCCESS");
        assertItem(result, 2, "DUPLICATE", "SKIPPED");
        assertItem(result, 3, "WILL_BIND", "PENDING_GROUP_MAPPING");
        verify(userRoleRepository, never()).save(any(AuthUserRole.class));
        verify(authorizationService, never()).assignRoleToUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void dryRunBatchAssignmentsUsesPendingGroupMappingForSentry() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.emptyList());

        BatchRoleAssignmentResult result = service.dryRunBatchAssignments("dga_role",
                request(ClusterEndpoint.AUTH_SENTRY, "user1"));

        assertThat(result.getWillBindCount()).isEqualTo(1);
        assertThat(result.getPendingGroupMappingCount()).isEqualTo(1);
        assertItem(result, 0, "WILL_BIND", "PENDING_GROUP_MAPPING");
        verify(userRoleRepository, never()).save(any(AuthUserRole.class));
    }

    @Test
    void dryRunBatchAssignmentsUsesSuccessForStarRocks() {
        givenRole(role(ClusterEndpoint.AUTH_STARROCKS_SQL));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.emptyList());

        BatchRoleAssignmentResult result = service.dryRunBatchAssignments("dga_role",
                request(ClusterEndpoint.AUTH_STARROCKS_SQL, "user1"));

        assertThat(result.getWillBindCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertItem(result, 0, "WILL_BIND", "SUCCESS");
        verify(userRoleRepository, never()).save(any(AuthUserRole.class));
        verify(authorizationService, never()).assignRoleToUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void batchAssignUsersAssignsStarRocksUsersAndReturnsSuccessCounts() {
        givenRole(role(ClusterEndpoint.AUTH_STARROCKS_SQL));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(eq("dga_role"), eq("USER"), anyString(), eq("ACTIVE")))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchRoleAssignmentResult result = service.batchAssignUsers("dga_role",
                request(ClusterEndpoint.AUTH_STARROCKS_SQL, "user1", "user2"));

        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getValidCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertItem(result, 0, "BOUND", "SUCCESS");
        assertItem(result, 1, "BOUND", "SUCCESS");
        verify(authorizationService).assignRoleToUser("CDH", "dga_role", "user1", ClusterEndpoint.AUTH_STARROCKS_SQL);
        verify(authorizationService).assignRoleToUser("CDH", "dga_role", "user2", ClusterEndpoint.AUTH_STARROCKS_SQL);
    }

    @Test
    void batchAssignUsersRecordsPendingGroupMappingForSentry() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("dga_role", "USER", "user1", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchRoleAssignmentResult result = service.batchAssignUsers("dga_role",
                request(ClusterEndpoint.AUTH_SENTRY, "user1"));

        assertThat(result.getPendingGroupMappingCount()).isEqualTo(1);
        assertItem(result, 0, "BOUND", "PENDING_GROUP_MAPPING");
        verify(authorizationService, never()).assignRoleToUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void batchAssignUsersContinuesWhenBackendAssignmentFails() {
        givenRole(role(ClusterEndpoint.AUTH_STARROCKS_SQL));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus(eq("dga_role"), eq("USER"), anyString(), eq("ACTIVE")))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("backend down"))
                .when(authorizationService).assignRoleToUser("CDH", "dga_role", "bad_user", ClusterEndpoint.AUTH_STARROCKS_SQL);

        BatchRoleAssignmentResult result = service.batchAssignUsers("dga_role",
                request(ClusterEndpoint.AUTH_STARROCKS_SQL, "bad_user", "good_user"));

        assertThat(result.getValidCount()).isEqualTo(2);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertItem(result, 0, "BOUND", "FAILED");
        assertItem(result, 1, "BOUND", "SUCCESS");
    }

    @Test
    void batchAssignUsersSkipsExistingSuccessOrPendingAssignments() {
        givenRole(role(ClusterEndpoint.AUTH_STARROCKS_SQL));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndStatus("dga_role", "USER", "ACTIVE"))
                .thenReturn(Arrays.asList(
                        existingAssignment("success_user", ClusterEndpoint.AUTH_STARROCKS_SQL, "SUCCESS"),
                        existingAssignment("pending_user", ClusterEndpoint.AUTH_STARROCKS_SQL, "PENDING_GROUP_MAPPING")));

        BatchRoleAssignmentResult result = service.batchAssignUsers("dga_role",
                request(ClusterEndpoint.AUTH_STARROCKS_SQL, "success_user", "pending_user"));

        assertThat(result.getAlreadyBoundCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getPendingGroupMappingCount()).isEqualTo(1);
        assertItem(result, 0, "ALREADY_BOUND", "SUCCESS");
        assertItem(result, 1, "ALREADY_BOUND", "PENDING_GROUP_MAPPING");
        verify(userRoleRepository, never()).save(any(AuthUserRole.class));
        verify(authorizationService, never()).assignRoleToUser(anyString(), anyString(), anyString(), anyString());
    }

    private void givenRole(AuthRole role) {
        when(roleRepository.findByRoleCode("dga_role")).thenReturn(role);
    }

    private AuthRole role(String authBackend) {
        AuthRole role = new AuthRole();
        role.setRoleCode("dga_role");
        role.setRoleName("Test Role");
        role.setCluster("CDH");
        role.setAuthBackend(authBackend);
        role.setStatus("ACTIVE");
        return role;
    }

    private BatchRoleAssignmentRequest request(String authBackend, String... usernames) {
        BatchRoleAssignmentRequest request = new BatchRoleAssignmentRequest();
        request.setCluster("CDH");
        request.setAuthBackend(authBackend);
        request.setUsernames(new ArrayList<>(Arrays.asList(usernames)));
        return request;
    }

    private AuthorizationCapability capability(String authBackend) {
        AuthorizationCapability capability = new AuthorizationCapability();
        AuthorizationCapability.RbacCapabilities rbac = new AuthorizationCapability.RbacCapabilities();
        if (ClusterEndpoint.AUTH_STARROCKS_SQL.equals(authBackend) || ClusterEndpoint.AUTH_DORIS_SQL.equals(authBackend)) {
            rbac.setSupportsNativeUserRole(true);
            rbac.setSupportsNativeGroupRole(true);
            rbac.setDefaultSubjectType("USER");
            rbac.setAllowedSubjectTypes(Arrays.asList("USER", "GROUP"));
        } else if (ClusterEndpoint.AUTH_RANGER.equals(authBackend)) {
            rbac.setUsesMaterializedPolicies(true);
            rbac.setUserAssignmentUsesMaterializedPolicies(true);
            rbac.setGroupAssignmentUsesMaterializedPolicies(true);
            rbac.setDefaultSubjectType("USER");
            rbac.setAllowedSubjectTypes(Arrays.asList("USER", "GROUP"));
        } else {
            rbac.setSupportsNativeGroupRole(true);
            rbac.setUserAssignmentUsesMaterializedPolicies(false);
            rbac.setDefaultSubjectType("GROUP");
            rbac.setAllowedSubjectTypes(Arrays.asList("USER", "GROUP"));
        }
        capability.setRbac(rbac);
        return capability;
    }

    private AuthUserRole existingAssignment(String username, String authBackend, String backendSyncStatus) {
        AuthUserRole assignment = new AuthUserRole();
        assignment.setRoleCode("dga_role");
        assignment.setCluster("CDH");
        assignment.setSubjectType("USER");
        assignment.setSubjectName(username);
        assignment.setAuthBackend(authBackend);
        assignment.setStatus("ACTIVE");
        assignment.setBackendSyncStatus(backendSyncStatus);
        assignment.setSyncMessage(backendSyncStatus + " existing");
        return assignment;
    }

    private void assertItem(BatchRoleAssignmentResult result, int index, String action, String status) {
        BatchRoleAssignmentItem item = result.getItems().get(index);
        assertThat(item.getAction()).isEqualTo(action);
        assertThat(item.getStatus()).isEqualTo(status);
    }
}
