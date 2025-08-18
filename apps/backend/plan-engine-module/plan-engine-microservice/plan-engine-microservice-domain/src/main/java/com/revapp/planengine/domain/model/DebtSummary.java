package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DebtSummary {
    @Builder.Default
    private List<DebtItem> items = new ArrayList<>();
    private BigDecimal totalRemaining; // opcional
}
