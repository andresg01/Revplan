package com.revplan.llm.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAdjustments {
    private BigDecimal savingPct;
    private Integer emergencyMonths;
    @Builder.Default private Map<String, BigDecimal> envelopes = new HashMap<>();
}
