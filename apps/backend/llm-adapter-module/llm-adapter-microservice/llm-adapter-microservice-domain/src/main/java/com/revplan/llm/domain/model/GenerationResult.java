package com.revplan.llm.domain.model;

import lombok.*;

/**
 * Resultado interno de la generación (útil para setear headers como X-Provider-Request-ID).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerationResult {
    private PlanLLMOutput output;
    private String providerRequestId; // para header X-Provider-Request-ID
    private Integer durationMs;       // para métricas
}
