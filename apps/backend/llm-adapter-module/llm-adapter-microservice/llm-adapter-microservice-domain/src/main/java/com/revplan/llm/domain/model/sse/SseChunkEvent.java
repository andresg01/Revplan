package com.revplan.llm.domain.model.sse;

import lombok.*;

/** Evento SSE 'chunk' con el delta textual/json bruto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SseChunkEvent {
    private String delta;
}
