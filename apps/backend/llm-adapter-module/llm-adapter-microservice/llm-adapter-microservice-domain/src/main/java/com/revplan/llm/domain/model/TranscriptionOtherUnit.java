package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.PriceUnitEnum;
import java.math.BigDecimal;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TranscriptionOtherUnit {
    private PriceUnitEnum unit;
    private BigDecimal amount;
}
