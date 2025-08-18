package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateConstraints {
    private BigDecimal minSavingPct;    // opcional
    private BigDecimal maxSavingPct;    // opcional
    private Integer minEmergencyMonths; // opcional
    private Integer maxEmergencyMonths; // opcional
}
