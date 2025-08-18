package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScorePreviewItem {
    private String templateId;
    private BigDecimal score; // 0..1
    @Builder.Default
    private Map<String, BigDecimal> factors = new HashMap<>();
}
