package com.dga.access.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.dga.cluster.entity.ClusterEndpoint;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RangerService {

    @Value("${ranger.url}")
    private String rangerUrl;

    @Value("${ranger.username:admin}")
    private String username;

    @Value("${ranger.password:Admini8888}")
    private String password;

    @Value("${ranger.service.name:hdp_hive}")
    private String serviceName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void grantPermission(String user, String database, String table, String permission) {
        grantPermission(user, database, table, permission, null);
    }

    public void grantPermission(String user, String database, String table, String permission, ClusterEndpoint endpoint) {
        RangerConfig config = config(endpoint);
        String finalTable = (table == null || table.isEmpty()) ? "*" : table;
        Map<String, Object> policy = findPolicy(config, database, finalTable);

        if (policy == null) {
            createPolicy(config, user, database, finalTable, permission);
        } else {
            updatePolicyGrant(config, policy, user, permission);
        }
    }

    public void revokePermission(String user, String database, String table, String permission) {
        revokePermission(user, database, table, permission, null);
    }

    public void revokePermission(String user, String database, String table, String permission, ClusterEndpoint endpoint) {
        RangerConfig config = config(endpoint);
        String finalTable = (table == null || table.isEmpty()) ? "*" : table;
        Map<String, Object> policy = findPolicy(config, database, finalTable);

        if (policy != null) {
            updatePolicyRevoke(config, policy, user);
        } else {
            System.out.println("Ranger policy not found for revoke: " + database + "." + finalTable);
        }
    }

    public List<Map<String, Object>> getUserPermissions(String user) {
        return getUserPermissions(user, null);
    }

    public List<Map<String, Object>> getUserPermissions(String user, ClusterEndpoint endpoint) {
        RangerConfig config = config(endpoint);
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String url = String.format("%s/service/public/v2/api/policy?serviceName=%s", config.url, config.serviceName);
            
            HttpHeaders headers = createHeaders(config);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> policies = response.getBody();
            
            if (policies != null) {
                for (Map<String, Object> policy : policies) {
                    if (!Boolean.TRUE.equals(policy.get("isEnabled"))) continue;
                    
                    Map<String, Object> resources = (Map<String, Object>) policy.get("resources");
                    String db = getResourceValue(resources, "database");
                    String table = getResourceValue(resources, "table");
                    
                    List<Map<String, Object>> policyItems = (List<Map<String, Object>>) policy.get("policyItems");
                    if (policyItems != null) {
                        for (Map<String, Object> item : policyItems) {
                            List<String> users = (List<String>) item.get("users");
                            if (users != null && users.contains(user)) {
                                List<Map<String, Object>> accesses = (List<Map<String, Object>>) item.get("accesses");
                                for (Map<String, Object> access : accesses) {
                                    if (Boolean.TRUE.equals(access.get("isAllowed"))) {
                                        Map<String, Object> perm = new HashMap<>();
                                        perm.put("database", db);
                                        perm.put("table", table);
                                        String type = (String) access.get("type");
                                        String mappedPerm = type.toUpperCase();
                                        if ("UPDATE".equals(mappedPerm)) {
                                            mappedPerm = "INSERT";
                                        }
                                        perm.put("permission", mappedPerm);
                                        result.add(perm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Ranger permissions: " + e.getMessage());
        }
        return result;
    }

    public List<String> listPolicyUsers(ClusterEndpoint endpoint) {
        RangerConfig config = config(endpoint);
        Set<String> users = new TreeSet<>();
        try {
            String url = String.format("%s/service/public/v2/api/policy?serviceName=%s", config.url, config.serviceName);
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(config)), List.class);
            List<Map<String, Object>> policies = response.getBody();
            if (policies == null) {
                return new ArrayList<>();
            }
            for (Map<String, Object> policy : policies) {
                collectUsers(users, (List<Map<String, Object>>) policy.get("policyItems"));
                collectUsers(users, (List<Map<String, Object>>) policy.get("denyPolicyItems"));
                collectUsers(users, (List<Map<String, Object>>) policy.get("allowExceptions"));
                collectUsers(users, (List<Map<String, Object>>) policy.get("denyExceptions"));
            }
        } catch (Exception e) {
            System.err.println("Error listing Ranger policy users: " + e.getMessage());
        }
        return new ArrayList<>(users);
    }

    private void collectUsers(Set<String> result, List<Map<String, Object>> policyItems) {
        if (policyItems == null) {
            return;
        }
        for (Map<String, Object> item : policyItems) {
            List<String> itemUsers = (List<String>) item.get("users");
            if (itemUsers != null) {
                for (String user : itemUsers) {
                    if (user != null && !user.trim().isEmpty() && !user.trim().startsWith("{")) {
                        result.add(user.trim());
                    }
                }
            }
        }
    }

    private String getResourceValue(Map<String, Object> resources, String key) {
        if (resources == null) return null;
        Map<String, Object> res = (Map<String, Object>) resources.get(key);
        if (res == null) return null;
        List<String> values = (List<String>) res.get("values");
        if (values != null && !values.isEmpty()) return values.get(0);
        return null;
    }

    private Map<String, Object> findPolicy(RangerConfig config, String database, String table) {
        try {
            // Ranger API search by resource is partial/contains match.
            // We must filter results to find EXACT match.
            String url = String.format("%s/service/public/v2/api/policy?serviceName=%s&resource:database=%s&resource:table=%s",
                    config.url, config.serviceName, database, table);
            
            HttpHeaders headers = createHeaders(config);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> policies = response.getBody();
            
            if (policies != null) {
                for (Map<String, Object> policy : policies) {
                    Map<String, Object> resources = (Map<String, Object>) policy.get("resources");
                    
                    String policyDb = getResourceValue(resources, "database");
                    String policyTable = getResourceValue(resources, "table");
                    
                    // Check for exact match (null safe)
                    boolean dbMatch = Objects.equals(database, policyDb);
                    boolean tableMatch = Objects.equals(table, policyTable);
                    
                    if (dbMatch && tableMatch) {
                        return policy;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching Ranger policy: " + e.getMessage());
        }
        return null;
    }

    private void createPolicy(RangerConfig config, String user, String database, String table, String permission) {
        try {
            Map<String, Object> policy = new HashMap<>();
            policy.put("service", config.serviceName);
            policy.put("name", "dga_auto_" + database + "_" + table.replace("*", "all") + "_" + System.currentTimeMillis());
            policy.put("isEnabled", true);

            Map<String, Object> resources = new HashMap<>();
            
            Map<String, Object> dbRes = new HashMap<>();
            dbRes.put("values", Collections.singletonList(database));
            dbRes.put("isExcludes", false);
            dbRes.put("isRecursive", false);
            resources.put("database", dbRes);

            Map<String, Object> tblRes = new HashMap<>();
            tblRes.put("values", Collections.singletonList(table));
            tblRes.put("isExcludes", false);
            tblRes.put("isRecursive", false);
            resources.put("table", tblRes);

            Map<String, Object> colRes = new HashMap<>();
            colRes.put("values", Collections.singletonList("*"));
            colRes.put("isExcludes", false);
            colRes.put("isRecursive", false);
            resources.put("column", colRes);

            policy.put("resources", resources);

            List<Map<String, Object>> policyItems = new ArrayList<>();
            policyItems.add(createPolicyItem(user, permission));
            policy.put("policyItems", policyItems);

            String url = config.url + "/service/public/v2/api/policy";
            HttpHeaders headers = createHeaders(config);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(policy, headers);

            System.out.println("Creating Ranger Policy. URL: " + url);
            System.out.println("Payload: " + policy);

            try {
                restTemplate.postForEntity(url, entity, Map.class);
            } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException ex) {
                System.err.println("Ranger API Error: " + ex.getStatusCode() + " " + ex.getStatusText());
                System.err.println("Response Body: " + ex.getResponseBodyAsString());
                throw ex;
            }
            
            System.out.println("Created Ranger policy for " + user + " on " + database + "." + table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Ranger policy: " + e.getMessage());
        }
    }

    private void updatePolicyGrant(RangerConfig config, Map<String, Object> policy, String user, String permission) {
        try {
            List<Map<String, Object>> policyItems = (List<Map<String, Object>>) policy.get("policyItems");
            if (policyItems == null) {
                policyItems = new ArrayList<>();
                policy.put("policyItems", policyItems);
            }

            // Strategy: Check if there is an item with exact same accesses.
            // If yes, add user to it.
            // If no, create new item.
            
            Set<String> targetAccesses = getAccessTypes(permission);
            boolean added = false;

            for (Map<String, Object> item : policyItems) {
                List<Map<String, Object>> accesses = (List<Map<String, Object>>) item.get("accesses");
                Set<String> itemAccessTypes = accesses.stream()
                    .map(a -> (String) a.get("type"))
                    .collect(Collectors.toSet());
                
                if (itemAccessTypes.equals(targetAccesses)) {
                    List<String> users = (List<String>) item.get("users");
                    if (users == null) {
                        users = new ArrayList<>();
                        item.put("users", users);
                    }
                    if (!users.contains(user)) {
                        users.add(user);
                    }
                    added = true;
                    break;
                }
            }

            if (!added) {
                policyItems.add(createPolicyItem(user, permission));
            }

            submitUpdate(config, policy);
            System.out.println("Granted Ranger policy for " + user);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update Ranger policy (Grant): " + e.getMessage());
        }
    }

    private void updatePolicyRevoke(RangerConfig config, Map<String, Object> policy, String user) {
        try {
            List<Map<String, Object>> policyItems = (List<Map<String, Object>>) policy.get("policyItems");
            if (policyItems == null) return;

            boolean changed = false;
            Iterator<Map<String, Object>> iterator = policyItems.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> item = iterator.next();
                List<String> users = (List<String>) item.get("users");
                if (users != null && users.contains(user)) {
                    users.remove(user);
                    changed = true;
                }
                // If item is empty (no users, no groups), remove it
                List<String> groups = (List<String>) item.get("groups");
                if ((users == null || users.isEmpty()) && (groups == null || groups.isEmpty())) {
                    iterator.remove();
                    changed = true;
                }
            }

            if (changed) {
                submitUpdate(config, policy);
                System.out.println("Revoked Ranger policy for " + user);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to update Ranger policy (Revoke): " + e.getMessage());
        }
    }

    private void submitUpdate(RangerConfig config, Map<String, Object> policy) {
        String url = config.url + "/service/public/v2/api/policy/" + policy.get("id");
        HttpHeaders headers = createHeaders(config);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(policy, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
    }

    private Map<String, Object> createPolicyItem(String user, String permission) {
        Map<String, Object> item = new HashMap<>();
        item.put("users", new ArrayList<>(Collections.singletonList(user)));
        item.put("accesses", getAccessesList(permission));
        item.put("delegateAdmin", false);
        return item;
    }

    private List<Map<String, Object>> getAccessesList(String permission) {
        Set<String> types = getAccessTypes(permission);
        List<Map<String, Object>> list = new ArrayList<>();
        for (String t : types) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", t);
            map.put("isAllowed", true);
            list.add(map);
        }
        return list;
    }
    
    private Set<String> getAccessTypes(String permission) {
        Set<String> perms = new HashSet<>();
        if ("ALL".equalsIgnoreCase(permission)) {
            perms.add("select");
            perms.add("update");
            perms.add("create");
            perms.add("drop");
            perms.add("alter");
            perms.add("index");
            perms.add("lock");
        } else if ("SELECT".equalsIgnoreCase(permission)) {
            perms.add("select");
        } else if ("INSERT".equalsIgnoreCase(permission)) {
            perms.add("update"); 
        } else if ("CREATE".equalsIgnoreCase(permission)) {
            perms.add("create");
        } else {
             perms.add(permission.toLowerCase());
        }
        return perms;
    }

    private HttpHeaders createHeaders() {
        return createHeaders(config(null));
    }

    private HttpHeaders createHeaders(RangerConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "curl/7.64.1"); // Mock curl
        headers.set("Accept", "*/*");
        headers.set("Connection", "close"); // Avoid keep-alive issues
        String auth = config.username + ":" + config.password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    private RangerConfig config(ClusterEndpoint endpoint) {
        RangerConfig config = new RangerConfig();
        config.url = trimTrailingSlash(endpoint != null && endpoint.getUrl() != null ? endpoint.getUrl() : rangerUrl);
        config.username = endpoint != null && endpoint.getUsername() != null ? endpoint.getUsername() : username;
        config.password = endpoint != null && endpoint.getPassword() != null ? endpoint.getPassword() : password;
        config.serviceName = endpoint != null && endpoint.getServiceName() != null && !endpoint.getServiceName().trim().isEmpty()
                ? endpoint.getServiceName().trim() : serviceName;
        return config;
    }

    private String trimTrailingSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static class RangerConfig {
        private String url;
        private String username;
        private String password;
        private String serviceName;
    }
}
