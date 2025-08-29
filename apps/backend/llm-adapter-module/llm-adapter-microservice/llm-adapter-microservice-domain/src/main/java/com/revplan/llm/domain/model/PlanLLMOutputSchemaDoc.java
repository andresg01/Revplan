// src/main/java/com/revplan/llm/domain/model/PlanLLMOutputSchemaDoc.java
package com.revplan.llm.domain.model;

import lombok.*;
import java.util.HashMap;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanLLMOutputSchemaDoc {
    private String title;
    private String type;

    /** additionalProperties del schema (opcional, passthrough). */
    @Builder.Default
    private Map<String, Object> additionalProperties = new HashMap<>();
}
