package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlanKPIs {
    /** 0..1 */
    private BigDecimal savingRate;
    /** meses, puede ser decimal (p.ej. 6.8) */
    private BigDecimal runwayMonths;
    /** 0..1 */
    private BigDecimal budgetCompliance;
}
