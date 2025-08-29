package com.revplan.llm.domain.model;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTriple {
    private BigDecimal input;        // nullable si no aplica
    private BigDecimal cachedInput;  // nullable
    private BigDecimal output;       // nullable
}