package com.revapp.planengine.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SpendSummary {
    private Integer periodDays;     // opcional (DTO)
    private BigDecimal totalOut;    // opcional (DTO usa Double; mapea a BigDecimal en dominio)
    @Builder.Default
    private List<SpendCategoryAmount> categories = new ArrayList<>();
}
