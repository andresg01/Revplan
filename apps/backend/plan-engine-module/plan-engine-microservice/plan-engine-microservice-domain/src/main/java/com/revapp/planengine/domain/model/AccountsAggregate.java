package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.CurrencyEnum;
import lombok.*;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsAggregate {
    private Money totalBalance; // opcional
    /** Suma por divisa (usa BigDecimal en dominio) */
    @Builder.Default
    private Map<CurrencyEnum, BigDecimal> byCurrency = new EnumMap<>(CurrencyEnum.class);
}
