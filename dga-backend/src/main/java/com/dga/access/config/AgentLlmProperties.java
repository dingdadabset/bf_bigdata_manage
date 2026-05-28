package com.dga.access.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dga.agent.llm")
public class AgentLlmProperties {

    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 60;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = providerValue(baseUrl, "https://api.deepseek.com");
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = providerValue(model, "deepseek-chat");
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds <= 0 ? 60 : timeoutSeconds;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    private String providerValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
