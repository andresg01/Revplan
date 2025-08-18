package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.SpendCategoryEnum;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SpendCategoryAmount {
    private SpendCategoryEnum category;
    private BigDecimal amount;
}
