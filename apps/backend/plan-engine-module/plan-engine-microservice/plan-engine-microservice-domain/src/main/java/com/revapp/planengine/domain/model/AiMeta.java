package com.revapp.planengine.domain.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMeta {
    private String provider;
    private String model;
    private String promptVersion;
    private Double temperature;
    private Integer tokensPrompt;
    private Integer tokensOutput;
    private Integer latencyMs;
}
