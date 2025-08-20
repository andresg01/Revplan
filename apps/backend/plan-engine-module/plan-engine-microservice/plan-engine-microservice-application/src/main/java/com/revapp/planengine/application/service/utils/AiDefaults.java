package com.revapp.planengine.application.service.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.defaults")
public class AiDefaults {
    private String provider = "openai";
    private String model = "gpt-5-mini";
    private Double temperature = 0.2;
    private String promptVersion = "2025-08-19";
}
