package com.revplan.llm.infra.persistence.jpa.utils;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        com.revplan.llm.infra.config.LimitsProperties.class
})
public class AdapterConfig {
}
