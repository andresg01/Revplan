package com.revplan.llm.domain.model;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineTuningItem {
    private String modelId;                   // opcional
    private BigDecimal trainingHourUSD;       // opcional
    private BigDecimal trainingPer1MTokensUSD;// opcional
    private PriceTriple inference;            // opcional
}