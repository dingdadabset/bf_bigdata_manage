package com.dga.access.service;

import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionPreview;
import com.dga.access.dto.HistoricalPermissionAdoptionPreviewRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionRequest;
import com.dga.access.dto.HistoricalPermissionAdoptionResult;
import com.dga.access.entity.AuthRole;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.entity.AuthUserRole;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.AuthRoleAssignmentAuditRepository;
import com.dga.access.repository.AuthRolePermissionRepository;
import com.dga.access.repository.AuthRoleRepository;
import com.dga.access.repository.AuthUserRoleRepository;
import com.dga.access.repository.UserResourceAccessRepository;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HistoricalPermissionAdoptionServiceTest {

    @Mock
    private AuthRoleRepository roleRepository;

    @Mock
    private AuthRolePermissionRepository permissionRepository;

    @Mock
    private UserResourceAccessRepository userResourceAccessRepository;

    @Mock
    private AuthUserRoleRepository userRoleRepository;

    @Mock
    private AuthRoleAssignmentAuditRepository auditRepository;

    @Mock
    private AuthorizationService authorizationService;

    private HistoricalPermissionAdoptionService service;
    private AuthRoleService authRoleService;

    @BeforeEach
    void setUp() {
        authRoleService = new AuthRoleService();
        ReflectionTestUtils.setField(authRoleService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(authRoleService, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(authRoleService, "userRoleRepository", userRoleRepository);
        ReflectionTestUtils.setField(authRoleService, "auditRepository", auditRepository);
        ReflectionTestUtils.setField(authRoleService, "authorizationService", authorizationService);

        service = new HistoricalPermissionAdoptionService();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "userResourceAccessRepository", userResourceAccessRepository);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "authRoleService", authRoleService);

        when(authorizationService.normalizeAuthBackend(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? null : String.valueOf((Object) invocation.getArgument(0)).trim().toUpperCase());
        when(authorizationService.authBackend(anyString())).thenReturn(ClusterEndpoint.AUTH_SENTRY);
        when(authorizationService.resolveClusterCodeOrName("CDH")).thenReturn("CDH");
        when(authorizationService.engineType("CDH", ClusterEndpoint.AUTH_SENTRY)).thenReturn("HIVE");
        when(roleRepository.findByRoleCode("dga_role")).thenReturn(role());
        when(userResourceAccessRepository.findByUsernameAndClusterCodeAndIsDeletedFalse("legacy_user", "CDH"))
                .thenReturn(Collections.emptyList());
        when(userResourceAccessRepository.findActiveRoleAdoptionRecords("legacy_user", "CDH", ClusterEndpoint.AUTH_SENTRY, "dga_role"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("dga_role", "USER", "legacy_user", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.findByRoleCodeAndSubjectTypeAndSubjectNameAndStatus("dga_role", "GROUP", "analytics", "ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(userRoleRepository.save(any(AuthUserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userResourceAccessRepository.save(any(UserResourceAccess.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void previewClassifiesExactMatchAndAdoptionWritesLocalRecordsOnly() {
        givenRolePermissions(permission("tmp_aggr_pay", null, "ALL"));
        givenLivePermissions(live("tmp_aggr_pay", null, "ALL", "USER", "legacy_user", null));

        HistoricalPermissionAdoptionPreview preview = service.preview("dga_role", previewRequest());

        assertThat(preview.getReconciliationStatus()).isEqualTo("EXACT_MATCH");
        assertThat(preview.getSummary().getAdoptableCount()).isEqualTo(1);
        assertThat(preview.getItems().get(0).getAdoptionStatus()).isEqualTo("DIRECT_MATCH");

        HistoricalPermissionAdoptionRequest request = adoptionRequest(preview);
        HistoricalPermissionAdoptionResult result = service.adopt("dga_role", request, "admin");

        assertThat(result.getAdoptedCount()).isEqualTo(1);
        assertThat(result.getAssignmentStatus()).isEqualTo("LOCAL_ONLY");
        verify(userRoleRepository).save(any(AuthUserRole.class));
        verify(userResourceAccessRepository).save(any(UserResourceAccess.class));
        verify(authorizationService, never()).grant(any(), anyString());
        verify(authorizationService, never()).revoke(any(), anyString());
        verify(authorizationService, never()).assignRoleToUser(anyString(), anyString(), anyString(), anyString());
        verify(authorizationService, never()).assignRoleToGroup(anyString(), anyString(), anyString(), anyString());
        verify(authorizationService, never()).grantPermissionToRole(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void subsetRequiresMissingPermissionAcknowledgement() {
        givenRolePermissions(permission("tmp_aggr_pay", null, "ALL"), permission("dw_pay", null, "SELECT"));
        givenLivePermissions(live("tmp_aggr_pay", null, "ALL", "USER", "legacy_user", null));

        HistoricalPermissionAdoptionPreview preview = service.preview("dga_role", previewRequest());

        assertThat(preview.getReconciliationStatus()).isEqualTo("SUBSET_OF_ROLE");
        HistoricalPermissionAdoptionRequest request = adoptionRequest(preview);
        request.setAllowRoleMissingBackendPermissions(false);

        assertThatThrownBy(() -> service.adopt("dga_role", request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不会自动补发缺失权限");
    }

    @Test
    void supersetRequiresExtraPermissionAcknowledgement() {
        givenRolePermissions(permission("tmp_aggr_pay", null, "ALL"));
        givenLivePermissions(live("tmp_aggr_pay", null, "ALL", "USER", "legacy_user", null),
                live("extra_db", null, "SELECT", "USER", "legacy_user", null));

        HistoricalPermissionAdoptionPreview preview = service.preview("dga_role", previewRequest());

        assertThat(preview.getReconciliationStatus()).isEqualTo("SUPERSET_OF_ROLE");
        HistoricalPermissionAdoptionRequest request = adoptionRequest(preview);
        request.setAllowSupersetExtrasUnmanaged(false);

        assertThatThrownBy(() -> service.adopt("dga_role", request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("角色外权限");
    }

    @Test
    void groupInheritedOnlyIsBlockedUnlessAcknowledged() {
        givenRolePermissions(permission("tmp_aggr_pay", null, "ALL"));
        givenLivePermissions(live("tmp_aggr_pay", null, "ALL", "GROUP_ROLE", "legacy_sentry_role", "analytics"));

        HistoricalPermissionAdoptionPreview preview = service.preview("dga_role", previewRequest());

        assertThat(preview.getReconciliationStatus()).isEqualTo("GROUP_INHERITED_ONLY");
        assertThat(preview.getSummary().getBlockedGroupInheritedCount()).isEqualTo(1);
        HistoricalPermissionAdoptionRequest request = adoptionRequest(preview);
        request.setAdoptGroupInheritedPermissions(false);
        request.setAcknowledgeGroupInherited(false);
        request.setRolePermissions(Collections.singletonList(selection(preview.getItems().get(0))));

        assertThatThrownBy(() -> service.adopt("dga_role", request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("LDAP 组继承权限");

        HistoricalPermissionAdoptionPreview acknowledgedPreview = service.preview("dga_role", previewRequest(true));
        HistoricalPermissionAdoptionRequest acknowledged = adoptionRequest(acknowledgedPreview);
        acknowledged.setAdoptGroupInheritedPermissions(true);
        acknowledged.setAcknowledgeGroupInherited(true);
        acknowledged.setRolePermissions(Collections.singletonList(selection(acknowledgedPreview.getItems().get(0))));

        HistoricalPermissionAdoptionResult result = service.adopt("dga_role", acknowledged, "admin");

        assertThat(result.getAdoptedCount()).isEqualTo(1);
        verify(userRoleRepository).save(any(AuthUserRole.class));
        verify(userResourceAccessRepository).save(any(UserResourceAccess.class));
        verify(authorizationService, never()).revoke(any(), anyString());
    }

    @Test
    void previewDetectsOverlapFromRawSentryGrantText() {
        givenRolePermissions(permission("baofoo_cm_v2", null, "SELECT"));
        Map<String, Object> rawGrant = new HashMap<>();
        rawGrant.put("grant", "GRANT SELECT ON DATABASE baofoo_cm_v2 TO ROLE role_test_new");
        rawGrant.put("source", "GROUP_ROLE");
        rawGrant.put("sourceRole", "role_test_new");
        rawGrant.put("sourceGroup", "test_new");
        givenLivePermissions(rawGrant);

        HistoricalPermissionAdoptionPreview preview = service.preview("dga_role", previewRequest(true));

        assertThat(preview.getReconciliationStatus()).isEqualTo("GROUP_INHERITED_ONLY");
        assertThat(preview.getSummary().getMatchedGroupInheritedCount()).isEqualTo(1);
        assertThat(preview.getSummary().getRolePermissionCount()).isEqualTo(1);
        assertThat(preview.getItems().get(0).getAdoptionStatus()).isEqualTo("GROUP_INHERITED_MATCH");
        assertThat(preview.getItems().get(0).getDatabaseName()).isEqualTo("baofoo_cm_v2");
    }

    @Test
    void snapshotMismatchBlocksPersistence() {
        givenRolePermissions(permission("tmp_aggr_pay", null, "ALL"));
        givenLivePermissions(live("tmp_aggr_pay", null, "ALL", "USER", "legacy_user", null));

        HistoricalPermissionAdoptionRequest request = adoptionRequest(service.preview("dga_role", previewRequest()));
        request.setExpectedSnapshotHash("stale");

        assertThatThrownBy(() -> service.adopt("dga_role", request, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("live 权限已变化");
        verify(userResourceAccessRepository, never()).save(any(UserResourceAccess.class));
    }

    private HistoricalPermissionAdoptionPreviewRequest previewRequest() {
        return previewRequest(false);
    }

    private HistoricalPermissionAdoptionPreviewRequest previewRequest(boolean includeGroupInherited) {
        HistoricalPermissionAdoptionPreviewRequest request = new HistoricalPermissionAdoptionPreviewRequest();
        request.setUsername("legacy_user");
        request.setCluster("CDH");
        request.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        request.setSubjectType("USER");
        request.setSubjectName("legacy_user");
        request.setIncludeGroupInherited(includeGroupInherited);
        return request;
    }

    private HistoricalPermissionAdoptionRequest adoptionRequest(HistoricalPermissionAdoptionPreview preview) {
        HistoricalPermissionAdoptionRequest request = new HistoricalPermissionAdoptionRequest();
        request.setUsername("legacy_user");
        request.setCluster("CDH");
        request.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        request.setSubjectType("USER");
        request.setSubjectName("legacy_user");
        request.setExpectedSnapshotHash(preview.getSnapshotHash());
        request.setBindRoleLocalOnly(true);
        request.setAdoptDirectUserPermissions(true);
        request.setAdoptUserRolePermissions(true);
        request.setAdoptGroupInheritedPermissions(false);
        request.setAcknowledgeGroupInherited(false);
        request.setAllowPartialAdoption(true);
        request.setAllowSupersetExtrasUnmanaged(true);
        request.setAllowRoleMissingBackendPermissions(true);
        List<BatchGrantRequest.RolePermissionSelection> selections = new ArrayList<>();
        for (HistoricalPermissionAdoptionPreview.Item item : preview.getItems()) {
            if (item.isAdoptable()) {
                selections.add(selection(item));
            }
        }
        request.setRolePermissions(selections);
        return request;
    }

    private BatchGrantRequest.RolePermissionSelection selection(HistoricalPermissionAdoptionPreview.Item item) {
        BatchGrantRequest.RolePermissionSelection selection = new BatchGrantRequest.RolePermissionSelection();
        selection.setResourceType(item.getResourceType());
        selection.setDatabaseName(item.getDatabaseName());
        selection.setTableName(item.getTableName());
        selection.setPermission(item.getPermission());
        selection.setAuthBackend(item.getAuthBackend());
        return selection;
    }

    private void givenRolePermissions(AuthRolePermission... permissions) {
        List<AuthRolePermission> items = new ArrayList<>();
        Collections.addAll(items, permissions);
        when(permissionRepository.findByRoleCodeAndStatus("dga_role", "ACTIVE")).thenReturn(items);
    }

    private void givenLivePermissions(Map<String, Object>... grants) {
        List<Map<String, Object>> items = new ArrayList<>();
        Collections.addAll(items, grants);
        when(authorizationService.getUserPermissions("legacy_user", "CDH", ClusterEndpoint.AUTH_SENTRY)).thenReturn(items);
    }

    private AuthRole role() {
        AuthRole role = new AuthRole();
        role.setRoleCode("dga_role");
        role.setRoleName("DGA Role");
        role.setCluster("CDH");
        role.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        role.setStatus("ACTIVE");
        return role;
    }

    private AuthRolePermission permission(String database, String table, String action) {
        AuthRolePermission permission = new AuthRolePermission();
        permission.setRoleCode("dga_role");
        permission.setCluster("CDH");
        permission.setResourceType(table == null ? "DATABASE" : "TABLE");
        permission.setDatabaseName(database);
        permission.setTableName(table);
        permission.setPermission(action);
        permission.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        permission.setStatus("ACTIVE");
        return permission;
    }

    private Map<String, Object> live(String database, String table, String action, String source, String sourceRole, String sourceGroup) {
        Map<String, Object> row = new HashMap<>();
        row.put("database", database);
        row.put("table", table);
        row.put("permission", action);
        row.put("resourceType", table == null ? "DATABASE" : "TABLE");
        row.put("authBackend", ClusterEndpoint.AUTH_SENTRY);
        row.put("source", source);
        row.put("sourceRole", sourceRole);
        row.put("sourceGroup", sourceGroup);
        row.put("grantText", "GRANT " + action + " ON DATABASE " + database);
        return row;
    }
}
