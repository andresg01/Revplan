package com.revplan.llm.domain.model;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextModelPrice {
    private String modelId;
    private PriceTriple price;
}