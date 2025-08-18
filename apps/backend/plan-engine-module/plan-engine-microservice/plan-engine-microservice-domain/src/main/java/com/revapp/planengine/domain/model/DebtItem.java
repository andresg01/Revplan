package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DebtItem {
    private String id;
    private BigDecimal apr;
    private BigDecimal remainingPrincipal;
    private BigDecimal minPayment; // opcional
}
