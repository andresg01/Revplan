package com.revplan.llm.infra.persistence.jpa.repository;

import com.revplan.llm.domain.model.HealthLLM;
import com.revplan.llm.domain.repository.HealthRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adapter simple que simula un “ping” al proveedor LLM.
 * Más adelante puedes sustituir el cuerpo por una llamada HTTP real.
 */
@Component
public class ProviderHealthAdapter implements HealthRepository {

    @Override
    public HealthLLM check() {
        return HealthLLM.builder()
                .provider("openai")     // o "azure_openai", según configuración futura
                .reachable(true)        // simulación básica
                .modelLatencyMs(120)    // simulación básica
                .timestamp(LocalDateTime.now())
                .build();
    }
}
