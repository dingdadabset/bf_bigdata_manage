package com.dga.access.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class IpaHttpService {

    @Value("${ipa.http.enabled:false}")
    private boolean enabled;

    @Value("${ipa.http.url:https://freeipa.baofoo.cn/ipa}")
    private String ipaUrl;

    @Value("${ipa.http.admin.user:admin}")
    private String adminUser;

    @Value("${ipa.http.admin.password:}")
    private String adminPassword;

    private CloseableHttpClient httpClient;
    private BasicCookieStore cookieStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (!enabled) return;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            // Trust ALL certificates (self-signed or not) to avoid PKIX path building issues
            builder.loadTrustMaterial(null, (chain, authType) -> true);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    builder.build(), NoopHostnameVerifier.INSTANCE);

            this.cookieStore = new BasicCookieStore();
            this.httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .setDefaultCookieStore(cookieStore)
                    .setRedirectStrategy(new LaxRedirectStrategy())
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SSL for IPA HTTP client", e);
        }
    }

    public void createUser(String username, String firstName, String lastName, String password) {
        if (!enabled) {
            throw new IllegalStateException("IPA HTTP is disabled");
        }
        
        // 1. Login to get session cookie
        login();

        // 2. Create User via JSON-RPC
        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "user_add/1");
        payload.put("id", 0);
        
        List<Object> params = new ArrayList<>();
        List<String> positionalParams = Collections.singletonList(username);
        params.add(positionalParams);
        
        Map<String, Object> kwParams = new HashMap<>();
        kwParams.put("givenname", firstName);
        kwParams.put("sn", lastName);
        kwParams.put("userpassword", password);
        kwParams.put("loginshell", "/bin/bash");
        // kwParams.put("all", true); // "all": true might return too much info, optional

        params.add(kwParams);
        payload.put("params", params);

        try {
            executeRpc(payload, "IPA User created via HTTP: " + username);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("already exists")) {
                System.out.println("IPA User " + username + " already exists. Treating as success.");
                return;
            }
            throw e;
        }
    }

    public void deleteUser(String username) {
        if (!enabled) {
            throw new IllegalStateException("IPA HTTP is disabled");
        }
        
        login();

        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "user_del");
        payload.put("id", 0);
        
        List<Object> params = new ArrayList<>();
        List<String> positionalParams = Collections.singletonList(username);
        params.add(positionalParams);
        
        Map<String, Object> kwParams = new HashMap<>();
        params.add(kwParams);
        
        payload.put("params", params);

        executeRpc(payload, "IPA User deleted via HTTP: " + username);
    }

    public boolean userExists(String username) {
        if (!enabled) {
            throw new IllegalStateException("IPA HTTP is disabled");
        }

        login();

        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "user_show");
        payload.put("id", 0);

        List<Object> params = new ArrayList<>();
        params.add(Collections.singletonList(username));
        params.add(new HashMap<String, Object>());
        payload.put("params", params);

        try {
            executeRpcWithResponse(payload, "IPA User checked via HTTP: " + username);
            return true;
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase(Locale.ROOT) : "";
            if (msg.contains("not found") || msg.contains("doesn't exist") || msg.contains("does not exist") || msg.contains("no such")) {
                return false;
            }
            throw e;
        }
    }

    public List<Map<String, Object>> listUsers() {
        if (!enabled) {
            throw new IllegalStateException("IPA HTTP is disabled");
        }

        login();

        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "user_find");
        payload.put("id", 0);

        List<Object> params = new ArrayList<>();
        params.add(Collections.singletonList("")); // Criteria: all
        Map<String, Object> kwParams = new HashMap<>();
        kwParams.put("sizelimit", 0); // No limit
        kwParams.put("timelimit", 0);
        params.add(kwParams);

        payload.put("params", params);

        Map<String, Object> response = executeRpcWithResponse(payload, "IPA User List fetched");
        
        // Parse response
        // Structure: { "result": { "result": [ { "uid": ["user1"], ... }, ... ], "count": 10, ... }, ... }
        Object resultObj = response.get("result");
        if (resultObj instanceof Map) {
            Object innerResult = ((Map<?, ?>) resultObj).get("result");
            if (innerResult instanceof List) {
                return (List<Map<String, Object>>) innerResult;
            }
        }
        return Collections.emptyList();
    }

    private void executeRpc(Map<String, Object> payload, String successMessage) {
        executeRpcWithResponse(payload, successMessage);
    }

    private Map<String, Object> executeRpcWithResponse(Map<String, Object> payload, String successMessage) {
        try {
            HttpPost request = new HttpPost(ipaUrl + "/session/json");
            request.setHeader("Referer", ipaUrl + "/ui/");
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            
            String jsonBody = objectMapper.writeValueAsString(payload);
            request.setEntity(new StringEntity(jsonBody));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseString = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode != 200) {
                    throw new RuntimeException("IPA HTTP Error: " + statusCode + " - " + responseString);
                }
                
                Map<String, Object> respMap = objectMapper.readValue(responseString, new TypeReference<Map<String, Object>>() {});
                if (respMap.get("error") != null) {
                    Object errObj = respMap.get("error");
                    String msg = String.valueOf(errObj);
                    if (errObj instanceof Map) {
                        Object message = ((Map<?, ?>) errObj).get("message");
                        if (message != null) {
                            msg = String.valueOf(message);
                        }
                    }
                    throw new RuntimeException("IPA API Error: " + msg);
                }
                
                System.out.println(successMessage);
                return respMap;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute IPA HTTP request", e);
        }
    }

    private void login() {
        try {
            cookieStore.clear();

            HttpPost loginRequest = new HttpPost(ipaUrl + "/session/login_password");
            loginRequest.setHeader("Referer", ipaUrl + "/ui/");
            loginRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
            loginRequest.setHeader("Accept", "text/plain");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("user", adminUser));
            params.add(new BasicNameValuePair("password", adminPassword));
            loginRequest.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(loginRequest)) {
                // We just need the cookie to be stored in cookieStore
                EntityUtils.consume(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new RuntimeException("IPA Login Failed: HTTP " + statusCode);
                }
            }

            boolean hasSessionCookie = cookieStore.getCookies().stream()
                    .anyMatch(c -> c != null && c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains("ipa_session"));
            if (!hasSessionCookie) {
                throw new RuntimeException("IPA Login Failed: session cookie not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("IPA Login Exception", e);
        }
    }
}
