package com.dga.access.service;

import com.dga.access.dto.AuthRoleAssignmentRequest;
import com.dga.access.dto.AuthRoleImportRequest;
import com.dga.access.dto.AuthRoleImportResult;
import com.dga.access.dto.AuthRolePermissionRequest;
import com.dga.access.dto.BackendRolePermissionSnapshot;
import com.dga.access.dto.BackendRoleSnapshot;
import com.dga.access.dto.BatchRoleAssignmentItem;
import com.dga.access.dto.BatchRoleAssignmentRequest;
import com.dga.access.dto.BatchRoleAssignmentResult;
import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthRolePermission;
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
    void addPermissionRejectsStarRocksWildcardDatabaseScope() {
        AuthRole role = role(ClusterEndpoint.AUTH_STARROCKS_SQL);
        role.setEngineType("STARROCKS");
        givenRole(role);

        AuthRolePermissionRequest request = new AuthRolePermissionRequest();
        request.setDatabaseName("*");
        request.setPermission("SELECT");

        assertThatThrownBy(() -> service.addPermission("dga_role", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("StarRocks 角色权限范围请指定具体数据库");
        verify(permissionRepository, never()).save(any(AuthRolePermission.class));
        verify(authorizationService, never()).grantPermissionToRole(anyString(), anyString(), anyString(), any(), anyString(), anyString());
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
    void addAssignmentRecordsSubsetControlledGroupBindingWithoutGrantingFullRole() {
        givenRole(role(ClusterEndpoint.AUTH_SENTRY));
        AuthUserRole[] saved = new AuthUserRole[1];
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("dga_role", "GROUP", "analytics", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            return saved[0];
        });
        when(permissionRepository.findByRoleCodeAndStatus("dga_role", "ACTIVE")).thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndStatus("dga_role", "ACTIVE")).thenReturn(Collections.emptyList());

        AuthRoleAssignmentRequest request = new AuthRoleAssignmentRequest();
        request.setSubjectType("GROUP");
        request.setSubjectName("analytics");
        request.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);

        service.addAssignment("dga_role", request);

        assertThat(saved[0].getBackendSyncStatus()).isEqualTo("LOCAL_ONLY");
        assertThat(saved[0].getSyncMessage()).contains("子集授权下发");
        verify(authorizationService, never()).assignRoleToGroup(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deletePermissionRevokesRangerMaterializedGroupPoliciesInsteadOfRolePermission() {
        AuthRole role = role(ClusterEndpoint.AUTH_RANGER);
        AuthRolePermission permission = permission(1L, role, "tmp_aggr_pay", null, "ALL");
        givenRole(role);
        when(permissionRepository.findById(1L)).thenReturn(java.util.Optional.of(permission));
        when(userRoleRepository.findByRoleCodeAndStatus("dga_role", "ACTIVE"))
                .thenReturn(Collections.singletonList(existingAssignment("analytics", ClusterEndpoint.AUTH_RANGER, "SUCCESS", "GROUP")));
        when(permissionRepository.save(any(AuthRolePermission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByRoleCodeAndStatus("dga_role", "ACTIVE")).thenReturn(Collections.emptyList());

        service.deletePermission("dga_role", 1L);

        assertThat(permission.getStatus()).isEqualTo("DELETED");
        verify(authorizationService).revokePermissionFromGroup("CDH", "analytics", "tmp_aggr_pay", null, "ALL", ClusterEndpoint.AUTH_RANGER);
        verify(authorizationService, never()).revokePermissionFromRole(anyString(), anyString(), anyString(), any(), anyString(), anyString());
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

    @Test
    void importBackendRolesCreatesLocalRecordsWithoutMutatingBackend() {
        BackendRoleSnapshot snapshot = backendRoleSnapshot("legacy_sentry_role");
        when(authorizationService.listBackendRoles(any())).thenReturn(Collections.singletonList(snapshot));
        when(roleRepository.findByRoleCode("legacy_sentry_role")).thenReturn(null);
        when(roleRepository.save(any(AuthRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByRoleCodeAndStatus("legacy_sentry_role", "ACTIVE")).thenReturn(new ArrayList<>());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("legacy_sentry_role", "GROUP", "analytics", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(permissionRepository.save(any(AuthRolePermission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthRoleImportResult result = service.importBackendRoles(importRequest("legacy_sentry_role"), "admin");

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getFailed()).isZero();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getPermissionCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getAssignmentCount()).isEqualTo(1);
        verify(authorizationService, never()).ensureRole(anyString(), anyString(), anyString());
        verify(authorizationService, never()).grantPermissionToRole(anyString(), anyString(), anyString(), any(), anyString(), anyString());
        verify(authorizationService, never()).assignRoleToGroup(anyString(), anyString(), anyString(), anyString());
        verify(authorizationService, never()).revokePermissionFromRole(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void importBackendRolesIsIdempotentForExistingPermissionAndAssignment() {
        BackendRoleSnapshot snapshot = backendRoleSnapshot("legacy_sentry_role");
        AuthRole existingRole = role(ClusterEndpoint.AUTH_SENTRY);
        existingRole.setRoleCode("legacy_sentry_role");
        when(authorizationService.listBackendRoles(any())).thenReturn(Collections.singletonList(snapshot));
        when(roleRepository.findByRoleCode("legacy_sentry_role")).thenReturn(existingRole);
        when(roleRepository.save(any(AuthRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByRoleCodeAndStatus("legacy_sentry_role", "ACTIVE"))
                .thenReturn(new ArrayList<>(Collections.singletonList(permission(1L, existingRole, "tmp_aggr_pay", null, "ALL"))));
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("legacy_sentry_role", "GROUP", "analytics", "ACTIVE"))
                .thenReturn(Collections.singletonList(existingAssignment("analytics", ClusterEndpoint.AUTH_SENTRY, "SUCCESS", "GROUP")));

        AuthRoleImportResult result = service.importBackendRoles(importRequest("legacy_sentry_role"), "admin");

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getCreated()).isZero();
        assertThat(result.getUpdated()).isZero();
        verify(permissionRepository, never()).save(any(AuthRolePermission.class));
        verify(userRoleRepository, never()).save(any(AuthUserRole.class));
        verify(authorizationService, never()).ensureRole(anyString(), anyString(), anyString());
    }

    private void givenRole(AuthRole role) {
        when(roleRepository.findByRoleCode("dga_role")).thenReturn(role);
    }

    private BackendRoleSnapshot backendRoleSnapshot(String roleCode) {
        BackendRoleSnapshot snapshot = new BackendRoleSnapshot();
        snapshot.setRoleCode(roleCode);
        snapshot.setRoleName(roleCode);
        snapshot.setCluster("CDH");
        snapshot.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        snapshot.setEngineType("HIVE");
        snapshot.setGroupNames(Collections.singletonList("analytics"));
        BackendRolePermissionSnapshot permission = new BackendRolePermissionSnapshot();
        permission.setResourceType("DATABASE");
        permission.setDatabaseName("tmp_aggr_pay");
        permission.setPermission("ALL");
        snapshot.setPermissions(Collections.singletonList(permission));
        return snapshot;
    }

    private AuthRoleImportRequest importRequest(String roleCode) {
        AuthRoleImportRequest request = new AuthRoleImportRequest();
        request.setCluster("CDH");
        request.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        request.setRoleCodes(Collections.singletonList(roleCode));
        request.setImportPermissions(true);
        request.setImportAssignments(true);
        return request;
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
        AuthorizationCapability.GrantCapabilities grant = new AuthorizationCapability.GrantCapabilities();
        grant.setSupportsRoleSubsetGrant(true);
        grant.setSupportsUserRoleSubsetGrant(true);
        grant.setSupportsGroupRoleSubsetGrant(true);
        capability.setRbac(rbac);
        capability.setGrant(grant);
        return capability;
    }

    private AuthRolePermission permission(Long id, AuthRole role, String database, String table, String action) {
        AuthRolePermission permission = new AuthRolePermission();
        permission.setId(id);
        permission.setRoleCode(role.getRoleCode());
        permission.setCluster(role.getCluster());
        permission.setResourceType(table == null ? "DATABASE" : "TABLE");
        permission.setDatabaseName(database);
        permission.setTableName(table);
        permission.setPermission(action);
        permission.setAuthBackend(role.getAuthBackend());
        permission.setStatus("ACTIVE");
        return permission;
    }

    private AuthUserRole existingAssignment(String username, String authBackend, String backendSyncStatus) {
        return existingAssignment(username, authBackend, backendSyncStatus, "USER");
    }

    private AuthUserRole existingAssignment(String name, String authBackend, String backendSyncStatus, String subjectType) {
        AuthUserRole assignment = new AuthUserRole();
        assignment.setRoleCode("dga_role");
        assignment.setCluster("CDH");
        assignment.setSubjectType(subjectType);
        assignment.setSubjectName(name);
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
