package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.CurrencyEnum;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Money {
    private BigDecimal amount;
    private CurrencyEnum currency;
}
