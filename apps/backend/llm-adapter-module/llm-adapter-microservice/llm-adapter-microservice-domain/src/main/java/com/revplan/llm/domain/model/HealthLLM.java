package com.revplan.llm.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthLLM {
    private String provider;
    private Boolean reachable;
    private Integer modelLatencyMs; // nullable
    private LocalDateTime timestamp;
}
