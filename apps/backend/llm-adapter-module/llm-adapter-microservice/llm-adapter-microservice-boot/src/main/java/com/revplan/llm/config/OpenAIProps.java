package com.revplan.llm.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "openai")
public class OpenAIProps {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String baseUrl = "https://api.openai.com/v1";

    @Min(1000)
    private Integer timeoutMs = 90_000;

    @Min(1024)
    private Integer maxInMemoryBytes = 2 * 1024 * 1024;

    // getters/setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public Integer getMaxInMemoryBytes() { return maxInMemoryBytes; }
    public void setMaxInMemoryBytes(Integer maxInMemoryBytes) { this.maxInMemoryBytes = maxInMemoryBytes; }
}
