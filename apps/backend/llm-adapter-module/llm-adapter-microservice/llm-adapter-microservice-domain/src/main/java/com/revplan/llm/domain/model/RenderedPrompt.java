package com.revplan.llm.domain.model;

import lombok.*;
import java.util.HashMap;
import java.util.Map;

/** Representa el prompt final enviado al proveedor (post-render de variables). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RenderedPrompt {
    private String systemPrompt;
    private String userMessage;
    /** Variables originales aportadas (debug/observabilidad). */
    @Builder.Default private Map<String, Object> variables = new HashMap<>();
}
