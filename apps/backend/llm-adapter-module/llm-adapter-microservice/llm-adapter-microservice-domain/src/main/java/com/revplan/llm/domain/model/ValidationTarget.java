package com.revplan.llm.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ValidationTarget {
    /** JSON Schema alternativo (si null, usar el oficial del plan). */
    private Object schema;
    /** Documento a validar. */
    private Object json;
}
