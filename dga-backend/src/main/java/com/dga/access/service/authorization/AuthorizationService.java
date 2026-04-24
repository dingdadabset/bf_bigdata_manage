package com.dga.access.service.authorization;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.cluster.repository.ClusterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuthorizationService {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private List<AuthorizationProvider> providers;

    public List<String> listDatabases(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).listDatabases(context);
    }

    public List<String> listTables(String clusterIdentifier, String database) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).listTables(context, database);
    }

    public List<String> listPrincipals(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).listPrincipals(context);
    }

    public List<Map<String, Object>> getUserPermissions(String username, String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).getUserPermissions(context, username);
    }

    public void grant(GrantCommand command) {
        AuthorizationContext context = buildContext(command.getCluster());
        selectProvider(context).grant(context, command);
    }

    public void revoke(RevokeCommand command) {
        AuthorizationContext context = buildContext(command.getCluster());
        selectProvider(context).revoke(context, command);
    }

    public void revokeAll(String username, String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        selectProvider(context).revokeAll(context, username);
    }

    public String engineType(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).engineType();
    }

    public String authBackend(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return selectProvider(context).authBackend();
    }

    public String resolveClusterCodeOrName(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        return context.getClusterCodeOrName();
    }

    public AuthorizationCapability capability(String clusterIdentifier) {
        AuthorizationContext context = buildContext(clusterIdentifier);
        Cluster cluster = context.getCluster();
        String type = cluster != null && cluster.getType() != null ? cluster.getType().toUpperCase() : "";

        if (type.contains("DORIS")) {
            return dorisCapability(context);
        }

        AuthorizationProvider provider;
        try {
            provider = selectProvider(context);
        } catch (RuntimeException e) {
            return unsupportedCapability(context, type);
        }
        if (ClusterEndpoint.AUTH_STARROCKS_SQL.equals(provider.authBackend())) {
            return starRocksCapability(context);
        }
        if (ClusterEndpoint.AUTH_RANGER.equals(provider.authBackend())) {
            return rangerCapability(context);
        }
        return hiveSentryCapability(context);
    }

    private AuthorizationContext buildContext(String clusterIdentifier) {
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
        context.setEndpoints(endpoints);
        return context;
    }

    private AuthorizationProvider selectProvider(AuthorizationContext context) {
        AuthorizationProvider fallback = null;
        for (AuthorizationProvider provider : providers) {
            if (!provider.supports(context)) {
                continue;
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
        capability.setPrincipalTypes(Arrays.asList("USER"));
        capability.setResourceTypes(Arrays.asList("DATABASE", "TABLE"));
        capability.setStatus("READY");
        return capability;
    }

    private AuthorizationCapability hiveSentryCapability(AuthorizationContext context) {
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType("HIVE");
        capability.setAuthBackend(ClusterEndpoint.AUTH_SENTRY);
        capability.setEndpointType(ClusterEndpoint.TYPE_HIVE_SERVER2);
        capability.setPermissions(Arrays.asList("SELECT", "INSERT", "CREATE", "ALL"));
        capability.setRequiresLdap(true);

        ClusterEndpoint hiveEndpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_HIVE_SERVER2);
        if (hiveEndpoint != null) {
            capability.setEndpointUrl(hiveEndpoint.getUrl());
        } else {
            capability.getWarnings().add("未配置 HIVE_SERVER2 端点，将尝试使用旧 hive.server2.* 配置。");
        }
        if (!AuthorizationSupport.hasEndpoint(context, ClusterEndpoint.TYPE_LDAP)) {
            capability.getWarnings().add("CDH/Hive 身份侧建议配置 LDAP 端点，用于用户创建、导入和查询。");
        }
        return capability;
    }

    private AuthorizationCapability starRocksCapability(AuthorizationContext context) {
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType("STARROCKS");
        capability.setAuthBackend(ClusterEndpoint.AUTH_STARROCKS_SQL);
        capability.setEndpointType(ClusterEndpoint.TYPE_STARROCKS_JDBC);
        capability.setPermissions(Arrays.asList("SELECT", "INSERT", "CREATE", "ALTER", "DROP", "ALL"));
        capability.setRequiresLdap(false);

        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_STARROCKS_JDBC);
        if (endpoint == null) {
            capability.setStatus("UNCONFIGURED");
            capability.getWarnings().add("StarRocks 授权需要 STARROCKS_JDBC 端点。");
        } else {
            capability.setEndpointUrl(endpoint.getUrl());
        }
        return capability;
    }

    private AuthorizationCapability rangerCapability(AuthorizationContext context) {
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType("HIVE");
        capability.setAuthBackend(ClusterEndpoint.AUTH_RANGER);
        capability.setEndpointType(ClusterEndpoint.TYPE_RANGER);
        capability.setPermissions(Arrays.asList("SELECT", "INSERT", "CREATE", "ALL"));
        capability.setRequiresLdap(true);

        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_RANGER);
        if (endpoint == null) {
            capability.setStatus("UNCONFIGURED");
            capability.getWarnings().add("Ranger 授权需要 RANGER 端点。");
        } else {
            capability.setEndpointUrl(endpoint.getUrl());
        }
        return capability;
    }

    private AuthorizationCapability dorisCapability(AuthorizationContext context) {
        AuthorizationCapability capability = baseCapability(context);
        capability.setEngineType("DORIS");
        capability.setAuthBackend(ClusterEndpoint.AUTH_DORIS_SQL);
        capability.setEndpointType(ClusterEndpoint.TYPE_DORIS_JDBC);
        capability.setPermissions(Arrays.asList("SELECT", "INSERT", "CREATE", "ALTER", "DROP", "ALL"));
        capability.setRequiresLdap(false);

        ClusterEndpoint endpoint = AuthorizationSupport.firstEndpoint(context, ClusterEndpoint.TYPE_DORIS_JDBC);
        if (endpoint != null) {
            capability.setEndpointUrl(endpoint.getUrl());
        }
        capability.setStatus("PLANNED");
        capability.getWarnings().add("Doris 授权适配器第一阶段仅预留能力声明，暂不执行真实授权。");
        if (endpoint == null) {
            capability.getWarnings().add("可先配置 DORIS_JDBC 端点，为后续真实授权做准备。");
        }
        return capability;
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
}
