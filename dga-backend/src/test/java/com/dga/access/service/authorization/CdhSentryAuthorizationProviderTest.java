package com.dga.access.service.authorization;

import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRoleSnapshot;
import com.dga.access.service.LdapService;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.service.HiveServer2ConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdhSentryAuthorizationProviderTest {

    @Mock
    private HiveServer2ConnectionService hiveServer2ConnectionService;

    @Mock
    private LdapService ldapService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CdhSentryAuthorizationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CdhSentryAuthorizationProvider();
        ReflectionTestUtils.setField(provider, "hiveServer2ConnectionService", hiveServer2ConnectionService);
        ReflectionTestUtils.setField(provider, "ldapService", ldapService);
        when(hiveServer2ConnectionService.jdbcTemplate(org.mockito.ArgumentMatchers.any())).thenReturn(jdbcTemplate);
    }

    @Test
    void listBackendRolesParsesSentryRolesPermissionsAndGroupBindings() {
        when(jdbcTemplate.queryForList("SHOW ROLES", String.class))
                .thenReturn(Arrays.asList("legacy_role", "other_role"));
        when(jdbcTemplate.queryForList("SHOW GRANT ROLE legacy_role"))
                .thenReturn(Collections.singletonList(row("grant", "GRANT ALL_PRIV ON DATABASE tmp_aggr_pay TO ROLE legacy_role")));
        when(jdbcTemplate.queryForList("SHOW ROLE GRANT GROUP analytics"))
                .thenReturn(Collections.singletonList(row("role", "legacy_role")));
        when(ldapService.listPosixGroups("CDH"))
                .thenReturn(Collections.singletonList(row("name", "analytics")));

        BackendRoleInventoryRequest request = new BackendRoleInventoryRequest();
        request.setCluster("CDH");
        request.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        request.setRoleCodes(Collections.singletonList("legacy_role"));

        List<BackendRoleSnapshot> snapshots = provider.listBackendRoles(context(), request);

        assertThat(snapshots).hasSize(1);
        BackendRoleSnapshot snapshot = snapshots.get(0);
        assertThat(snapshot.getRoleCode()).isEqualTo("legacy_role");
        assertThat(snapshot.getGroupNames()).containsExactly("analytics");
        assertThat(snapshot.getPermissions()).hasSize(1);
        assertThat(snapshot.getPermissions().get(0).getDatabaseName()).isEqualTo("tmp_aggr_pay");
        assertThat(snapshot.getPermissions().get(0).getTableName()).isNull();
        assertThat(snapshot.getPermissions().get(0).getPermission()).isEqualTo("ALL");
    }

    private AuthorizationContext context() {
        Cluster cluster = new Cluster();
        cluster.setClusterCode("CDH");
        cluster.setClusterName("CDH");
        cluster.setType("CDH");
        ClusterEndpoint endpoint = new ClusterEndpoint();
        endpoint.setEndpointType(ClusterEndpoint.TYPE_HIVE_SERVER2);
        endpoint.setUrl("jdbc:hive2://localhost:10000/default");
        AuthorizationContext context = new AuthorizationContext();
        context.setCluster(cluster);
        context.setClusterIdentifier("CDH");
        context.setRequestedAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        context.setEndpoints(Collections.singletonList(endpoint));
        return context;
    }

    private Map<String, Object> row(String key, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put(key, value);
        return row;
    }
}
