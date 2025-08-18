package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlanAdjustments {
    /** 0..1 */
    private BigDecimal savingPct;
    /** 0..24 */
    private Integer emergencyMonths;
    /** claves tipo "fixed", "variable", "goals" → valores 0..1 */
    private Map<String, BigDecimal> envelopes;
}
