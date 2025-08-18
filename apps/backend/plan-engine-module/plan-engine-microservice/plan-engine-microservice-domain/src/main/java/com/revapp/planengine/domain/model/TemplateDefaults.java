package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateDefaults {
    private BigDecimal savingPct;              // opcional
    private Integer emergencyMonths;           // opcional
    private Map<String, BigDecimal> envelopes = new HashMap<>();
}
