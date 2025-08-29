package com.revplan.llm.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerationParams {
    private String modelId;
    @Builder.Default private BigDecimal temperature = new BigDecimal("0.2");
    @Builder.Default private BigDecimal topP = new BigDecimal("1");
    @Builder.Default private Integer maxOutputTokens = 2048;
    private Integer seed;
    @Builder.Default private BigDecimal presencePenalty = new BigDecimal("0");
    @Builder.Default private BigDecimal frequencyPenalty = new BigDecimal("0");
}
