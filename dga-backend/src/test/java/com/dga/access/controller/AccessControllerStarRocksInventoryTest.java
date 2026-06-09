package com.dga.access.controller;

import com.dga.access.dto.StarRocksPermissionInventoryRequest;
import com.dga.access.entity.UserResourceAccess;
import com.dga.access.repository.UserResourceAccessRepository;
import com.dga.access.service.AdminGuard;
import com.dga.access.service.authorization.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessControllerStarRocksInventoryTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserResourceAccessRepository userResourceAccessRepository;

    @Mock
    private AdminGuard adminGuard;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AccessController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessController();
        ReflectionTestUtils.setField(controller, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(controller, "userResourceAccessRepository", userResourceAccessRepository);
        ReflectionTestUtils.setField(controller, "adminGuard", adminGuard);
    }

    @Test
    @SuppressWarnings("unchecked")
    void inventoryGroupsUsersIntoTemplateCandidates() {
        String cluster = "STARROCKS";
        String authBackend = "STARROCKS_SQL";

        when(authorizationService.authBackend(cluster)).thenReturn(authBackend);
        when(authorizationService.normalizeAuthBackend(authBackend)).thenReturn(authBackend);
        when(authorizationService.engineType(cluster, authBackend)).thenReturn("STARROCKS");
        when(authorizationService.resolveClusterCodeOrName(cluster)).thenReturn(cluster);
        when(authorizationService.listPrincipals(cluster, authBackend))
                .thenReturn(java.util.Arrays.asList("alice", "bob", "carol"));

        when(authorizationService.getUserPermissions("alice", cluster, authBackend))
                .thenReturn(readonlyGrants("alice"));
        when(authorizationService.getUserPermissions("bob", cluster, authBackend))
                .thenReturn(readonlyGrants("bob"));
        when(authorizationService.getUserPermissions("carol", cluster, authBackend))
                .thenReturn(writeGrants("carol"));

        StarRocksPermissionInventoryRequest request = new StarRocksPermissionInventoryRequest();
        request.setCluster(cluster);
        request.setIncludeRecordedGrants(false);

        Map<String, Object> result = controller.inventoryStarRocksPermissions(request, httpServletRequest);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("scannedUserCount")).isEqualTo(3);
        assertThat(result.get("templateCandidateCount")).isEqualTo(2);

        List<Map<String, Object>> templateCandidates = (List<Map<String, Object>>) result.get("templateCandidates");
        assertThat(templateCandidates).hasSize(2);
        assertThat(templateCandidates.get(0).get("userCount")).isEqualTo(2);
        assertThat((List<String>) templateCandidates.get(0).get("usernames")).containsExactly("alice", "bob");
        assertThat(templateCandidates.get(0).get("permissionCategory")).isEqualTo("READONLY");
        assertThat(String.valueOf(templateCandidates.get(0).get("templateCodeSuggestion"))).startsWith("sr_mixed_readonly_");

        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        assertThat(users).hasSize(3);
        assertThat(users.get(0).get("username")).isEqualTo("alice");
        assertThat(users.get(0).get("grantCount")).isEqualTo(2);
        assertThat(users.get(2).get("permissionCategory")).isEqualTo("READ_WRITE");
    }

    @Test
    void inventoryRejectsNonStarRocksCluster() {
        String cluster = "CDH";
        String authBackend = "SENTRY";

        when(authorizationService.authBackend(cluster)).thenReturn(authBackend);
        when(authorizationService.normalizeAuthBackend(authBackend)).thenReturn(authBackend);
        when(authorizationService.engineType(cluster, authBackend)).thenReturn("HIVE");

        StarRocksPermissionInventoryRequest request = new StarRocksPermissionInventoryRequest();
        request.setCluster(cluster);

        assertThatThrownBy(() -> controller.inventoryStarRocksPermissions(request, httpServletRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("不是 StarRocks 授权后端");
    }

    @Test
    @SuppressWarnings("unchecked")
    void inventoryFallsBackToRecordedUsersWhenPrincipalListingFails() {
        String cluster = "STARROCKS";
        String authBackend = "STARROCKS_SQL";
        UserResourceAccess recorded = new UserResourceAccess();
        recorded.setUsername("recorded_user");
        recorded.setClusterCode(cluster);
        recorded.setAuthBackend(authBackend);
        recorded.setStatus("ACTIVE");
        recorded.setDeleted(false);

        doNothing().when(adminGuard).requirePlatformAdmin(httpServletRequest, "仅 admin 或超级用户可执行 StarRocks 权限盘点");
        when(authorizationService.authBackend(cluster)).thenReturn(authBackend);
        when(authorizationService.normalizeAuthBackend(authBackend)).thenReturn(authBackend);
        when(authorizationService.engineType(cluster, authBackend)).thenReturn("STARROCKS");
        when(authorizationService.resolveClusterCodeOrName(cluster)).thenReturn(cluster);
        when(authorizationService.listPrincipals(cluster, authBackend)).thenThrow(new RuntimeException());
        when(userResourceAccessRepository.findActiveByCluster(cluster)).thenReturn(Collections.singletonList(recorded));
        when(authorizationService.getUserPermissions("recorded_user", cluster, authBackend))
                .thenReturn(readonlyGrants("recorded_user"));

        StarRocksPermissionInventoryRequest request = new StarRocksPermissionInventoryRequest();
        request.setCluster(cluster);
        request.setIncludeRecordedGrants(false);

        Map<String, Object> result = controller.inventoryStarRocksPermissions(request, httpServletRequest);

        assertThat(result.get("status")).isEqualTo("SUCCESS");
        assertThat(result.get("scannedUserCount")).isEqualTo(1);
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        assertThat(users).hasSize(1);
        assertThat(users.get(0).get("username")).isEqualTo("recorded_user");
    }

    private List<Map<String, Object>> readonlyGrants(String username) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(grant("GRANT SELECT ON ALL TABLES IN DATABASE `ods` TO USER '" + username + "'@'%'", null));
        rows.add(grant("GRANT SELECT ON TABLE `ads`.`orders` TO USER '" + username + "'@'%'", null));
        return rows;
    }

    private List<Map<String, Object>> writeGrants(String username) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(grant("GRANT SELECT ON ALL TABLES IN DATABASE `ads` TO USER '" + username + "'@'%'", null));
        rows.add(grant("GRANT INSERT ON TABLE `ads`.`orders` TO USER '" + username + "'@'%'", null));
        return rows;
    }

    private Map<String, Object> grant(String grantText, String source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Grants", grantText);
        if (source != null) {
            row.put("source", source);
        }
        return row;
    }
}
