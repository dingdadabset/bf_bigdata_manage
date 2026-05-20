package com.dga.access.service.authorization;

import com.dga.access.dto.BackendRoleInventoryRequest;
import com.dga.access.dto.BackendRoleSnapshot;
import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthorizationService {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private List<AuthorizationProvider> providers;

    public List<String> listDatabases(String clusterIdentifier) {
        return listDatabases(clusterIdentifier, null);
    }

    public List<String> listDatabases(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).listDatabases(context);
    }

    public List<String> listTables(String clusterIdentifier, String database) {
        return listTables(clusterIdentifier, database, null);
    }

    public List<String> listTables(String clusterIdentifier, String database, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).listTables(context, database);
    }

    public List<String> listPrincipals(String clusterIdentifier) {
        return listPrincipals(clusterIdentifier, null);
    }

    public List<String> listPrincipals(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).listPrincipals(context);
    }

    public List<String> listGroups(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).listGroups(context);
    }

    public List<BackendRoleSnapshot> listBackendRoles(BackendRoleInventoryRequest request) {
        AuthorizationContext context = buildContext(request == null ? null : request.getCluster(),
                request == null ? null : request.getAuthBackend());
        return selectProvider(context).listBackendRoles(context, request);
    }

    public boolean userExists(String clusterIdentifier, String username, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).userExists(context, username);
    }

    public List<Map<String, Object>> getUserPermissions(String username, String clusterIdentifier) {
        return getUserPermissions(username, clusterIdentifier, null);
    }

    public List<Map<String, Object>> getUserPermissions(String username, String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).getUserPermissions(context, username);
    }

    public void grant(GrantCommand command) {
        AuthorizationContext context = buildContext(command.getCluster());
        selectProvider(context).grant(context, command);
    }

    public void grant(GrantCommand command, String authBackend) {
        AuthorizationContext context = buildContext(command.getCluster(), authBackend);
        selectProvider(context).grant(context, command);
    }

    public void revoke(RevokeCommand command) {
        AuthorizationContext context = buildContext(command.getCluster());
        selectProvider(context).revoke(context, command);
    }

    public void revoke(RevokeCommand command, String authBackend) {
        AuthorizationContext context = buildContext(command.getCluster(), authBackend);
        selectProvider(context).revoke(context, command);
    }

    public void revokeAll(String username, String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).revokeAll(context, username);
    }

    public void revokeAll(String username, String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).revokeAll(context, username);
    }

    public void createUser(String clusterIdentifier, String username, String password) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).createUser(context, username, password);
    }

    public void ensureRole(String clusterIdentifier, String roleCode) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).ensureRole(context, roleCode);
    }

    public void ensureRole(String clusterIdentifier, String roleCode, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).ensureRole(context, roleCode);
    }

    public void grantPermissionToRole(String clusterIdentifier, String roleCode, String database, String table, String permission) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).grantPermissionToRole(context, roleCode, database, table, permission);
    }

    public void grantPermissionToRole(String clusterIdentifier, String roleCode, String database, String table,
                                      String permission, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).grantPermissionToRole(context, roleCode, database, table, permission);
    }

    public void grantPermissionToGroup(String clusterIdentifier, String groupName, String database, String table, String permission) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).grantPermissionToGroup(context, groupName, database, table, permission);
    }

    public void grantPermissionToGroup(String clusterIdentifier, String groupName, String database, String table,
                                       String permission, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).grantPermissionToGroup(context, groupName, database, table, permission);
    }

    public void revokePermissionFromGroup(String clusterIdentifier, String groupName, String database, String table, String permission) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).revokePermissionFromGroup(context, groupName, database, table, permission);
    }

    public void revokePermissionFromGroup(String clusterIdentifier, String groupName, String database, String table,
                                          String permission, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).revokePermissionFromGroup(context, groupName, database, table, permission);
    }

    public void revokePermissionFromRole(String clusterIdentifier, String roleCode, String database, String table, String permission) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).revokePermissionFromRole(context, roleCode, database, table, permission);
    }

    public void revokePermissionFromRole(String clusterIdentifier, String roleCode, String database, String table,
                                         String permission, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).revokePermissionFromRole(context, roleCode, database, table, permission);
    }

    public void assignRoleToUser(String clusterIdentifier, String roleCode, String username) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).assignRoleToUser(context, roleCode, username);
    }

    public void assignRoleToUser(String clusterIdentifier, String roleCode, String username, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).assignRoleToUser(context, roleCode, username);
    }

    public void assignRoleToGroup(String clusterIdentifier, String roleCode, String groupName) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).assignRoleToGroup(context, roleCode, groupName);
    }

    public void assignRoleToGroup(String clusterIdentifier, String roleCode, String groupName, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).assignRoleToGroup(context, roleCode, groupName);
    }

    public void revokeRoleAssignment(String clusterIdentifier, String roleCode, String subjectType, String subjectName) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).revokeRoleAssignment(context, roleCode, subjectType, subjectName);
    }

    public void revokeRoleAssignment(String clusterIdentifier, String roleCode, String subjectType, String subjectName, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        selectProvider(context).revokeRoleAssignment(context, roleCode, subjectType, subjectName);
    }

    public String engineType(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).engineType();
    }

    public String engineType(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).engineType();
    }

    public String authBackend(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).authBackend();
    }

    public String authBackend(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return selectProvider(context).authBackend();
    }

    public String normalizeAuthBackend(String authBackend) {
        return authBackend == null ? null : authBackend.trim().toUpperCase();
    }

    public String resolveClusterCodeOrName(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return context.getClusterCodeOrName();
    }

    public AuthorizationCapability capability(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return capability(context);
    }

    public AuthorizationCapability capability(String clusterIdentifier, String authBackend) {
        AuthorizationContext context = buildContext(clusterIdentifier, authBackend);
        return capability(context);
    }

    public boolean requiresExistingBackendUser(String clusterIdentifier, String authBackend) {
        AuthorizationCapability capability = capability(clusterIdentifier, authBackend);
        if (capability == null || capability.getRbac() == null || capability.getIdentity() == null) {
            return false;
        }
        String identityMode = firstNonBlank(capability.getIdentity().getMode(), capability.getIdentity().getUserSource());
        return capability.getRbac().isSupportsNativeUserRole() && "AUTH_BACKEND".equalsIgnoreCase(identityMode);
    }

    public List<AuthorizationCapability> backendCapabilities(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        List<AuthorizationCapability> capabilities = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (AuthorizationProvider provider : matchingProviders(context)) {
            String authBackend = normalizeAuthBackend(provider.authBackend());
            if (authBackend != null && !seen.add(authBackend)) {
                continue;
            }
            capabilities.add(capability(contextForBackend(context, provider.authBackend())));
        }
        if (!capabilities.isEmpty()) {
            return capabilities;
        }
        Cluster cluster = context.getCluster();
        String type = cluster != null && cluster.getType() != null ? cluster.getType().toUpperCase() : "";
        capabilities.add(unsupportedCapability(context, type));
        return capabilities;
    }

    private AuthorizationCapability capability(AuthorizationContext context) {
        try {
            return capabilityFromDescriptor(context, selectProvider(context));
        } catch (RuntimeException e) {
            Cluster cluster = context.getCluster();
            String type = cluster != null && cluster.getType() != null ? cluster.getType().toUpperCase() : "";
            return unsupportedCapability(context, type);
        }
    }

    private List<AuthorizationProvider> matchingProviders(AuthorizationContext context) {
        List<AuthorizationProvider> matches = new ArrayList<>();
        for (AuthorizationProvider provider : providers) {
            if (provider.supports(context)) {
                matches.add(provider);
            }
        }
        return matches;
    }

    private AuthorizationContext contextForBackend(AuthorizationContext context, String authBackend) {
        AuthorizationContext scoped = new AuthorizationContext();
        scoped.setCluster(context.getCluster());
        scoped.setClusterIdentifier(context.getClusterIdentifier());
        scoped.setRequestedAuthBackend(normalizeAuthBackend(authBackend));
        scoped.setEndpoints(context.getEndpoints());
        return scoped;
    }

    private AuthorizationContext buildContext(String clusterIdentifier) {
        return buildContext(clusterIdentifier, null);
    }

    private AuthorizationContext buildContext(String clusterIdentifier, String requestedAuthBackend) {
        String identifier = clusterIdentifier == null || clusterIdentifier.trim().isEmpty()
                ? "CDH-Cluster-01" : clusterIdentifier.trim();
        Cluster cluster = clusterRepository.findByClusterCode(identifier);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(identifier);
        }

        List<ClusterEndpoint> endpoints = new ArrayList<>();
        if (cluster != null && cluster.getClusterCode() != null) {
            endpoints = endpointRepository.findByClusterCodeAndStatusNot(cluster.getClusterCode(), "DELETED");
        }

        AuthorizationContext context = new AuthorizationContext();
        context.setCluster(cluster);
        context.setClusterIdentifier(identifier);
        context.setRequestedAuthBackend(normalizeAuthBackend(requestedAuthBackend));
        context.setEndpoints(endpoints);
        return context;
    }

    private AuthorizationProvider selectProvider(AuthorizationContext context) {
        AuthorizationProvider fallback = null;
        String requestedAuthBackend = context.getRequestedAuthBackend();
        for (AuthorizationProvider provider : providers) {
            if (!provider.supports(context)) {
                continue;
            }
            if (requestedAuthBackend != null) {
                if (requestedAuthBackend.equalsIgnoreCase(provider.authBackend())) {
                    return provider;
                }
                continue;
            }
            if (ClusterEndpoint.AUTH_DORIS_SQL.equals(provider.authBackend())) {
                return provider;
            }
            if (ClusterEndpoint.AUTH_STARROCKS_SQL.equals(provider.authBackend())) {
                return provider;
            }
            if (ClusterEndpoint.AUTH_RANGER.equals(provider.authBackend())) {
                fallback = provider;
            } else if (fallback == null) {
                fallback = provider;
            }
        }
        if (requestedAuthBackend != null) {
            throw new RuntimeException("No authorization provider found for backend: "
                    + requestedAuthBackend + " in cluster: " + context.getClusterIdentifier());
        }
        if (fallback != null) {
            return fallback;
        }
        throw new RuntimeException("No authorization provider found for cluster: " + context.getClusterIdentifier());
    }

    private AuthorizationCapability baseCapability(AuthorizationContext context) {
        AuthorizationCapability capability = new AuthorizationCapability();
        Cluster cluster = context.getCluster();
        capability.setCluster(context.getClusterCodeOrName());
        if (cluster != null) {
            capability.setClusterCode(cluster.getClusterCode());
            capability.setClusterName(cluster.getClusterName());
        }
        capability.setStatus("READY");
        return capability;
    }

    private AuthorizationCapability capabilityFromDescriptor(AuthorizationContext context, AuthorizationProvider provider) {
        AuthorizationProviderDescriptor descriptor = provider.descriptor();
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType(descriptor.getEngineType());
        capability.setAuthBackend(descriptor.getAuthBackend());
        capability.setEndpointType(descriptor.getEndpointType());
        capability.setPrincipalTypes(descriptor.getPrincipalTypes());
        capability.setResourceTypes(descriptor.getResourceTypes());
        capability.setPermissions(descriptor.getPermissions());
        capability.setRequiresLdap(descriptor.isRequiresLdap());
        capability.setIdentity(descriptor.getIdentity());
        capability.setRbac(descriptor.getRbac());
        capability.setGrant(descriptor.getGrant());
        capability.setUi(descriptor.getUi());

        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, descriptor.getEndpointType());
        if (endpoint != null) {
            capability.setEndpointUrl(endpoint.getUrl());
        } else if (descriptor.isEndpointRequired()) {
            capability.setStatus("UNCONFIGURED");
            String warning = descriptor.getMissingEndpointWarning();
            capability.getWarnings().add(warning == null || warning.trim().isEmpty()
                    ? descriptor.getEndpointType() + " 端点未配置。" : warning);
        }
        addProviderWarnings(context, capability);
        return capability;
    }

    private void addProviderWarnings(AuthorizationContext context, AuthorizationCapability capability) {
        if (capability.isRequiresLdap() && !AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_LDAP)) {
            capability.getWarnings().add("身份侧建议配置 LDAP 端点，用于用户创建、导入和查询。");
        }
        if (ClusterEndpoint.AUTH_SENTRY.equalsIgnoreCase(capability.getAuthBackend())
                && !AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2)) {
            capability.getWarnings().add("未配置 HIVE_SERVER2 端点，将尝试使用旧 hive.server2.* 配置。");
        }
        if (ClusterEndpoint.AUTH_RANGER.equalsIgnoreCase(capability.getAuthBackend())
                && !AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2)) {
            capability.getWarnings().add("未配置 HIVE_SERVER2 端点，库表下拉将从 Ranger 已有策略资源推断。");
        }
    }

    private AuthorizationCapability unsupportedCapability(AuthorizationContext context, String type) {
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType(type == null || type.isEmpty() ? "UNKNOWN" : type);
        capability.setAuthBackend("UNSUPPORTED");
        capability.setEndpointType("UNSUPPORTED");
        capability.setPermissions(new ArrayList<>());
        capability.setRequiresLdap(false);
        capability.setStatus("UNCONFIGURED");
        capability.getWarnings().add("当前环境暂未匹配到可用授权适配器，请配置 HIVE_SERVER2、STARROCKS_JDBC、RANGER 或 DORIS_JDBC 端点。");
        return capability;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
