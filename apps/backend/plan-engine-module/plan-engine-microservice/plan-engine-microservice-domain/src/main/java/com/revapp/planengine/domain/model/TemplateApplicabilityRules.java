package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class TemplateApplicabilityRules {
    private Integer minRisk;             // opcional
    private Integer maxRisk;             // opcional
    private BigDecimal maxDebtSeverity;  // opcional
    private BigDecimal minLiquidityNeed; // opcional
    private Integer minAge;              // opcional
    private Integer maxAge;              // opcional
}
