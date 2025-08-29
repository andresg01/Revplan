package com.revplan.llm.domain.model;

import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingPrice {
    private BigDecimal input;
}
