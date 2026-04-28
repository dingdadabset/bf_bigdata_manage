package com.dga.cluster.service;

import com.dga.cluster.entity.Cluster;
import com.dga.cluster.entity.ClusterEndpoint;
import com.dga.cluster.repository.ClusterRepository;
import com.dga.cluster.repository.ClusterEndpointRepository;
import com.dga.lineage.repository.DataLineageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.naming.ldap.Rdn;

@Service
public class ClusterService {

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private ClusterEndpointRepository endpointRepository;

    @Autowired
    private DataLineageRepository dataLineageRepository;

    private static final Pattern NON_CODE_CHARS = Pattern.compile("[^A-Z0-9_]+");

    public List<Cluster> getAllClusters() {
        List<Cluster> clusters = clusterRepository.findActiveClusters();
        for (Cluster cluster : clusters) {
            attachEndpoints(cluster);
        }
        return clusters;
    }

    public List<Cluster> getActiveClusters() {
        return clusterRepository.findByStatus("ACTIVE");
    }

    public Cluster getClusterById(Long id) {
        Cluster cluster = clusterRepository.findById(id).orElse(null);
        attachEndpoints(cluster);
        return cluster;
    }

    @Transactional
    public Cluster createCluster(Cluster cluster) {
        normalizeClusterCode(cluster);
        // Ensure clusterName is unique
        Cluster existing = clusterRepository.findByClusterName(cluster.getClusterName());
        if (existing != null) {
            // Check if it's a deleted cluster, we can reactivate it
            if ("DELETED".equals(existing.getStatus())) {
                existing.setStatus("ACTIVE");
                existing.setType(cluster.getType());
                existing.setDescription(cluster.getDescription());
                Cluster saved = clusterRepository.save(existing);
                saveEndpoints(saved.getClusterCode(), cluster.getEndpoints());
                attachEndpoints(saved);
                return saved;
            }
            throw new RuntimeException("Cluster name already exists");
        }
        if (clusterRepository.findByClusterCode(cluster.getClusterCode()) != null) {
            throw new RuntimeException("Cluster code already exists");
        }
        Cluster saved = clusterRepository.save(cluster);
        saveEndpoints(saved.getClusterCode(), cluster.getEndpoints());
        attachEndpoints(saved);
        return saved;
    }

    @Transactional
    public Cluster updateCluster(Long id, Cluster cluster) {
        Optional<Cluster> existing = clusterRepository.findById(id);
        if (existing.isPresent()) {
            Cluster c = existing.get();
            c.setClusterName(cluster.getClusterName());
            if (c.getClusterCode() == null || c.getClusterCode().trim().isEmpty()) {
                c.setClusterCode(cluster.getClusterCode());
                normalizeClusterCode(c);
            }
            c.setType(cluster.getType());
            c.setDescription(cluster.getDescription());
            c.setStatus(cluster.getStatus());
            Cluster saved = clusterRepository.save(c);
            saveEndpoints(saved.getClusterCode(), cluster.getEndpoints());
            attachEndpoints(saved);
            return saved;
        }
        return null;
    }

    public void deleteCluster(Long id) {
        Optional<Cluster> existing = clusterRepository.findById(id);
        if (existing.isPresent()) {
            Cluster c = existing.get();
            c.setStatus("DELETED");
            clusterRepository.save(c);
        }
    }

    public Cluster resolveCluster(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        Cluster cluster = clusterRepository.findByClusterCode(identifier);
        if (cluster == null) {
            cluster = clusterRepository.findByClusterName(identifier);
        }
        attachEndpoints(cluster);
        return cluster;
    }

    public List<ClusterEndpoint> getEndpoints(String clusterCode) {
        return endpointRepository.findByClusterCodeAndStatusNot(clusterCode, "DELETED");
    }

    public Map<String, Object> testEndpoint(String clusterCode, ClusterEndpoint endpoint) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        try {
            ClusterEndpoint testEndpoint = mergeEndpointSecret(clusterCode, endpoint);
            if (isBlank(testEndpoint.getEndpointType())) {
                throw new IllegalArgumentException("请选择端点类型");
            }
            if (isBlank(testEndpoint.getUrl())) {
                throw new IllegalArgumentException("请填写连接地址");
            }

            switch (testEndpoint.getEndpointType()) {
                case ClusterEndpoint.TYPE_HIVE_SERVER2:
                    testJdbc("org.apache.hive.jdbc.HiveDriver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_HIVE_METASTORE_DB:
                    testJdbc("com.mysql.cj.jdbc.Driver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_AZKABAN_DB:
                    testJdbc("com.mysql.cj.jdbc.Driver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_DOLPHINSCHEDULER_DB:
                    testJdbc("com.mysql.cj.jdbc.Driver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_STARROCKS_JDBC:
                    testJdbc("com.mysql.cj.jdbc.Driver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_DORIS_JDBC:
                    testJdbc("com.mysql.cj.jdbc.Driver", testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_LDAP:
                    testLdap(testEndpoint);
                    break;
                case ClusterEndpoint.TYPE_RANGER:
                    testRanger(testEndpoint);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的端点类型: " + testEndpoint.getEndpointType());
            }
            result.put("success", true);
            result.put("message", "连接成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        result.put("elapsedMs", System.currentTimeMillis() - start);
        return result;
    }

    @Transactional
    public ClusterEndpoint saveEndpoint(String clusterCode, ClusterEndpoint endpoint) {
        endpoint.setClusterCode(clusterCode);
        if (endpoint.getId() != null) {
            ClusterEndpoint existing = endpointRepository.findById(endpoint.getId())
                    .orElseThrow(() -> new RuntimeException("Endpoint not found"));
            existing.setEndpointType(endpoint.getEndpointType());
            existing.setAuthBackend(endpoint.getAuthBackend());
            existing.setUrl(endpoint.getUrl());
            existing.setUsername(endpoint.getUsername());
            if (endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
                existing.setPassword(endpoint.getPassword());
            }
            existing.setServiceName(endpoint.getServiceName());
            existing.setBaseDn(endpoint.getBaseDn());
            existing.setUserBaseDn(endpoint.getUserBaseDn());
            existing.setStatus(endpoint.getStatus());
            existing.setDescription(endpoint.getDescription());
            ClusterEndpoint saved = endpointRepository.save(existing);
            expireLineageIfSchedulerDisabled(saved);
            return saved;
        }
        ClusterEndpoint saved = endpointRepository.save(endpoint);
        expireLineageIfSchedulerDisabled(saved);
        return saved;
    }

    @Transactional
    public void deleteEndpoint(Long endpointId) {
        ClusterEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));
        endpoint.setStatus("DELETED");
        endpointRepository.save(endpoint);
        expireSchedulerLineage(endpoint);
    }

    private void expireLineageIfSchedulerDisabled(ClusterEndpoint endpoint) {
        if (endpoint != null && !"ACTIVE".equals(endpoint.getStatus())) {
            expireSchedulerLineage(endpoint);
        }
    }

    private void expireSchedulerLineage(ClusterEndpoint endpoint) {
        if (endpoint == null || endpoint.getId() == null || !isSchedulerEndpoint(endpoint.getEndpointType())) {
            return;
        }
        dataLineageRepository.updateActiveStatusBySourceEndpointId(endpoint.getId(), "EXPIRED");
    }

    private boolean isSchedulerEndpoint(String endpointType) {
        return ClusterEndpoint.TYPE_AZKABAN_DB.equals(endpointType)
                || ClusterEndpoint.TYPE_DOLPHINSCHEDULER_DB.equals(endpointType);
    }

    private void saveEndpoints(String clusterCode, List<ClusterEndpoint> endpoints) {
        if (endpoints == null) {
            return;
        }
        for (ClusterEndpoint endpoint : endpoints) {
            saveEndpoint(clusterCode, endpoint);
        }
    }

    private void attachEndpoints(Cluster cluster) {
        if (cluster == null || cluster.getClusterCode() == null) {
            return;
        }
        cluster.setEndpoints(getEndpoints(cluster.getClusterCode()));
    }

    private void normalizeClusterCode(Cluster cluster) {
        if (cluster.getClusterCode() != null && !cluster.getClusterCode().trim().isEmpty()) {
            cluster.setClusterCode(cluster.getClusterCode().trim().toUpperCase(Locale.ROOT));
            return;
        }
        String source = cluster.getClusterName() != null ? cluster.getClusterName() : "CLUSTER";
        String code = NON_CODE_CHARS.matcher(source.trim().toUpperCase(Locale.ROOT)).replaceAll("_");
        code = code.replaceAll("_+", "_");
        if (code.startsWith("_")) {
            code = code.substring(1);
        }
        if (code.endsWith("_")) {
            code = code.substring(0, code.length() - 1);
        }
        cluster.setClusterCode(code.isEmpty() ? "CLUSTER" : code);
    }

    private ClusterEndpoint mergeEndpointSecret(String clusterCode, ClusterEndpoint endpoint) {
        endpoint.setClusterCode(clusterCode);
        if (endpoint.getId() == null) {
            return endpoint;
        }
        Optional<ClusterEndpoint> existing = endpointRepository.findById(endpoint.getId());
        if (!existing.isPresent()) {
            return endpoint;
        }
        ClusterEndpoint stored = existing.get();
        if (isBlank(endpoint.getPassword())) {
            endpoint.setPassword(stored.getPassword());
        }
        if (isBlank(endpoint.getUrl())) {
            endpoint.setUrl(stored.getUrl());
        }
        if (isBlank(endpoint.getUsername())) {
            endpoint.setUsername(stored.getUsername());
        }
        if (isBlank(endpoint.getBaseDn())) {
            endpoint.setBaseDn(stored.getBaseDn());
        }
        if (isBlank(endpoint.getUserBaseDn())) {
            endpoint.setUserBaseDn(stored.getUserBaseDn());
        }
        if (isBlank(endpoint.getServiceName())) {
            endpoint.setServiceName(stored.getServiceName());
        }
        return endpoint;
    }

    private void testJdbc(String driverClassName, ClusterEndpoint endpoint) throws Exception {
        Class.forName(driverClassName);
        DriverManager.setLoginTimeout(5);
        try (Connection ignored = DriverManager.getConnection(
                endpoint.getUrl(),
                nullToEmpty(endpoint.getUsername()),
                nullToEmpty(endpoint.getPassword()))) {
            // Opening and closing a connection is enough for endpoint reachability.
        }
    }

    private void testLdap(ClusterEndpoint endpoint) {
        Exception lastError = null;
        List<String> candidates = ldapBindDnCandidates(endpoint);
        for (String userDn : candidates) {
            try {
                LdapContextSource source = new LdapContextSource();
                source.setUrl(endpoint.getUrl());
                if (!isBlank(endpoint.getBaseDn())) {
                    source.setBase(endpoint.getBaseDn().trim());
                }
                if (!isBlank(userDn)) {
                    source.setUserDn(userDn);
                }
                if (!isBlank(endpoint.getPassword())) {
                    source.setPassword(endpoint.getPassword());
                }
                source.afterPropertiesSet();
                new LdapTemplate(source).lookup("");
                return;
            } catch (Exception e) {
                lastError = e;
            }
        }
        String message = lastError == null ? "LDAP 连接失败" : lastError.getMessage();
        throw new IllegalStateException("LDAP 认证失败，请检查账号是否为完整 Bind DN 或密码是否正确。尝试账号: "
                + String.join(" / ", candidates) + "；错误: " + message, lastError);
    }

    private List<String> ldapBindDnCandidates(ClusterEndpoint endpoint) {
        String username = endpoint.getUsername() == null ? "" : endpoint.getUsername().trim();
        if (username.isEmpty()) {
            return Collections.singletonList("");
        }
        if (username.contains("=")) {
            return Collections.singletonList(username);
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String escaped = Rdn.escapeValue(username).toString();
        String baseDn = trimToNull(endpoint.getBaseDn());
        String userBaseDn = normalizeUserBaseDn(endpoint.getUserBaseDn(), baseDn);

        if (!isBlank(baseDn)) {
            candidates.add("cn=" + escaped + "," + baseDn);
        }
        if (!isBlank(userBaseDn)) {
            candidates.add("uid=" + escaped + "," + userBaseDn);
            candidates.add("cn=" + escaped + "," + userBaseDn);
        }
        candidates.add(username);
        return new ArrayList<>(candidates);
    }

    private String normalizeUserBaseDn(String userBaseDn, String baseDn) {
        String value = trimToNull(userBaseDn);
        if (value == null) {
            return baseDn;
        }
        if (isBlank(baseDn) || value.toLowerCase(Locale.ROOT).endsWith("," + baseDn.toLowerCase(Locale.ROOT))) {
            return value;
        }
        return value + "," + baseDn;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void testRanger(ClusterEndpoint endpoint) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json,*/*");
        if (!isBlank(endpoint.getUsername())) {
            String token = endpoint.getUsername() + ":" + nullToEmpty(endpoint.getPassword());
            String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        String baseUrl = trimTrailingSlash(endpoint.getUrl());
        String testUrl = isBlank(endpoint.getServiceName())
                ? baseUrl + "/service/public/v2/api/service"
                : baseUrl + "/service/public/v2/api/policy?serviceName=" + endpoint.getServiceName();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    testUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Ranger API 状态码: " + response.getStatusCodeValue());
            }
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("Ranger API 访问失败: HTTP " + e.getRawStatusCode()
                    + "，请检查账号密码、Ranger 地址和 serviceName。响应: " + e.getResponseBodyAsString(), e);
        }
    }

    private String trimTrailingSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
