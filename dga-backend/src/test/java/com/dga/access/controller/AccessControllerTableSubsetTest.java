package com.dga.access.controller;

import com.dga.access.dto.BatchGrantRequest;
import com.dga.access.entity.AuthRolePermission;
import com.dga.access.repository.AuthRolePermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessControllerTableSubsetTest {

    @Mock
    private AuthRolePermissionRepository authRolePermissionRepository;

    private AccessController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessController();
        ReflectionTestUtils.setField(controller, "authRolePermissionRepository", authRolePermissionRepository);
    }

    private AuthRolePermission databasePermission(String roleCode, String database, String permission, String authBackend) {
        AuthRolePermission p = new AuthRolePermission();
        p.setId(1L);
        p.setRoleCode(roleCode);
        p.setResourceType("DATABASE");
        p.setDatabaseName(database);
        p.setTableName(null);
        p.setPermission(permission);
        p.setAuthBackend(authBackend);
        p.setStatus("ACTIVE");
        return p;
    }

    @SuppressWarnings("unchecked")
    private List<AuthRolePermission> invokeValidateTableSubsetSelections(String roleCode,
                                                                        List<BatchGrantRequest.RolePermissionSelection> selections,
                                                                        String authBackend) throws Exception {
        Method method = AccessController.class.getDeclaredMethod(
                "validateTableSubsetSelections", String.class, List.class, String.class);
        method.setAccessible(true);
        try {
            return (List<AuthRolePermission>) method.invoke(controller, roleCode, selections, authBackend);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ResponseStatusException) {
                throw (ResponseStatusException) e.getCause();
            }
            throw e;
        }
    }

    private BatchGrantRequest.RolePermissionSelection tableSelection(String database, String table, String permission, boolean expanded) {
        BatchGrantRequest.RolePermissionSelection s = new BatchGrantRequest.RolePermissionSelection();
        s.setResourceType("TABLE");
        s.setDatabaseName(database);
        s.setTableName(table);
        s.setPermission(permission);
        s.setExpandedFromDatabasePermission(expanded);
        return s;
    }

    private BatchGrantRequest.RolePermissionSelection dbSelection(String database, String permission) {
        BatchGrantRequest.RolePermissionSelection s = new BatchGrantRequest.RolePermissionSelection();
        s.setResourceType("DATABASE");
        s.setDatabaseName(database);
        s.setTableName(null);
        s.setPermission(permission);
        s.setExpandedFromDatabasePermission(false);
        return s;
    }

    @Test
    void tableSelectionWithExpandedFlagPassesWhenParentDatabasePermissionExists() throws Exception {
        String roleCode = "role_analyst";
        AuthRolePermission dbPerm = databasePermission(roleCode, "ods", "SELECT", "SENTRY");
        when(authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE"))
                .thenReturn(Collections.singletonList(dbPerm));

        BatchGrantRequest.RolePermissionSelection selection = tableSelection("ods", "user_info", "SELECT", true);
        List<AuthRolePermission> result = invokeValidateTableSubsetSelections(roleCode, Collections.singletonList(selection), "SENTRY");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceType()).isEqualTo("TABLE");
        assertThat(result.get(0).getDatabaseName()).isEqualTo("ods");
        assertThat(result.get(0).getTableName()).isEqualTo("user_info");
        assertThat(result.get(0).getPermission()).isEqualTo("SELECT");
    }

    @Test
    void tableSelectionWithAllPermissionParentSubsumesSelect() throws Exception {
        String roleCode = "role_admin";
        AuthRolePermission dbPerm = databasePermission(roleCode, "ods", "ALL", "SENTRY");
        when(authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE"))
                .thenReturn(Collections.singletonList(dbPerm));

        BatchGrantRequest.RolePermissionSelection selection = tableSelection("ods", "orders", "SELECT", true);
        List<AuthRolePermission> result = invokeValidateTableSubsetSelections(roleCode, Collections.singletonList(selection), "SENTRY");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermission()).isEqualTo("SELECT");
        assertThat(result.get(0).getTableName()).isEqualTo("orders");
    }

    @Test
    void tableSelectionWithoutExpandedFlagRequiresExactMatch() {
        String roleCode = "role_analyst";
        AuthRolePermission dbPerm = databasePermission(roleCode, "ods", "SELECT", "SENTRY");
        when(authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE"))
                .thenReturn(Collections.singletonList(dbPerm));

        BatchGrantRequest.RolePermissionSelection selection = tableSelection("ods", "user_info", "SELECT", false);

        assertThatThrownBy(() -> invokeValidateTableSubsetSelections(roleCode, Collections.singletonList(selection), "SENTRY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("所选权限不属于角色");
    }

    @Test
    void tableSelectionForDatabaseNotInRoleIsRejected() {
        String roleCode = "role_analyst";
        AuthRolePermission dbPerm = databasePermission(roleCode, "ods", "SELECT", "SENTRY");
        when(authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE"))
                .thenReturn(Collections.singletonList(dbPerm));

        BatchGrantRequest.RolePermissionSelection selection = tableSelection("dwd", "user_info", "SELECT", true);

        assertThatThrownBy(() -> invokeValidateTableSubsetSelections(roleCode, Collections.singletonList(selection), "SENTRY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不在角色");
    }

    @Test
    void mixedSelectionsWithDatabaseAndExpandedTableBothValidate() throws Exception {
        String roleCode = "role_analyst";
        AuthRolePermission dbPerm = databasePermission(roleCode, "ods", "SELECT", "SENTRY");
        when(authRolePermissionRepository.findByRoleCodeAndStatus(roleCode, "ACTIVE"))
                .thenReturn(Collections.singletonList(dbPerm));

        BatchGrantRequest.RolePermissionSelection dbSel = dbSelection("ods", "SELECT");
        dbSel.setAuthBackend("SENTRY");
        BatchGrantRequest.RolePermissionSelection tableSel = tableSelection("ods", "orders", "SELECT", true);

        List<AuthRolePermission> result = invokeValidateTableSubsetSelections(roleCode, Arrays.asList(dbSel, tableSel), "SENTRY");

        assertThat(result).hasSize(2);
    }

    @Test
    void emptySelectionsThrows() {
        String roleCode = "role_analyst";
        assertThatThrownBy(() -> invokeValidateTableSubsetSelections(roleCode, Collections.emptyList(), "SENTRY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("请选择至少一项角色权限");
    }

    @Test
    void isTableSubsetRequestDetectsTableSubsetMode() {
        BatchGrantRequest request = new BatchGrantRequest();
        request.setTableSubsetMode(true);

        Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(controller, "isTableSubsetRequest", request);
        assertThat(result).isTrue();
    }

    @Test
    void isTableSubsetRequestDetectsExpandedFlag() {
        BatchGrantRequest request = new BatchGrantRequest();
        BatchGrantRequest.RolePermissionSelection sel = new BatchGrantRequest.RolePermissionSelection();
        sel.setExpandedFromDatabasePermission(true);
        request.setRolePermissions(Collections.singletonList(sel));

        Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(controller, "isTableSubsetRequest", request);
        assertThat(result).isTrue();
    }

    @Test
    void isTableSubsetRequestReturnsFalseForNormalRequest() {
        BatchGrantRequest request = new BatchGrantRequest();
        request.setRoleSubsetMode(true);
        BatchGrantRequest.RolePermissionSelection sel = new BatchGrantRequest.RolePermissionSelection();
        sel.setResourceType("DATABASE");
        sel.setDatabaseName("ods");
        sel.setPermission("SELECT");
        request.setRolePermissions(Collections.singletonList(sel));

        Boolean result = (Boolean) ReflectionTestUtils.invokeMethod(controller, "isTableSubsetRequest", request);
        assertThat(result).isFalse();
    }
}
