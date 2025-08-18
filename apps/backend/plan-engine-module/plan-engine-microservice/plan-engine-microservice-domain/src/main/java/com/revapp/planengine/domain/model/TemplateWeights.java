package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateWeights {
    private BigDecimal risk;
    private BigDecimal liquidity;
    private BigDecimal debt;
    private BigDecimal savingsGap; // <- antes 'gap'
    private BigDecimal age;
    private BigDecimal stability;
}
