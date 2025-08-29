package com.revplan.llm.domain.model.sse;

import lombok.*;

/** Evento SSE 'finish' con metadatos de la llamada al proveedor. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SseFinishEvent {
    private String providerRequestId;
    private Integer durationMs;
}
