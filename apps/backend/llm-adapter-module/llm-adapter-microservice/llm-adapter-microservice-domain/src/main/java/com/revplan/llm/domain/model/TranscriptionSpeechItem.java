package com.revplan.llm.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TranscriptionSpeechItem {
    private String modelId;
    private PriceTriple textTokens;                 // opcional
    private PriceTriple audioTokens;                // opcional
    @Builder.Default private List<TranscriptionOtherUnit> otherUnits = new ArrayList<>();
    private BigDecimal estimatedCostPerMinuteUSD;   // opcional
}
