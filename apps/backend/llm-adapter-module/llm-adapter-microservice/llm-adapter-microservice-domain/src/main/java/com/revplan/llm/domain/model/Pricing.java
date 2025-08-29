package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.PriceUnitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pricing {
    private PriceUnitEnum unit;
    private BigDecimal amount;
    private String notes; // opcional
}