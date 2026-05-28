package com.dga.access.service;

import com.dga.access.config.AgentLlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

@Service
public class DeepSeekClient {

    @Autowired
    private AgentLlmProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeepSeek API Key 未配置，请设置 DEEPSEEK_API_KEY");
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.getTimeoutSeconds() * 1000);
            factory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getModel());
            body.put("temperature", 0.2);
            body.put("stream", false);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(message("system", systemPrompt));
            messages.add(message("user", userPrompt));
            body.put("messages", messages);

            String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
            String response = restTemplate.postForObject(baseUrl + "/chat/completions", new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.asText("");
            if (text.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回内容为空");
            }
            return text;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "DeepSeek 调用失败: " + safeMessage(e), e);
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
    }
}
