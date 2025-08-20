package com.revapp.planengine.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AiResult {
    /** Fragmento/ajustes sugeridos (rationale, alerts, adjustments, kpis...). */
    private Plan planPatch;
    /** Metadatos de la inferencia (tokens, latencia, modelo…). */
    private AiMeta meta;
}
