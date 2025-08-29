package com.revplan.llm.domain.model;
import java.util.*;

import com.revplan.llm.domain.enums.PriceUnitEnum;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokensCategory {
    private PriceUnitEnum unit;
    @Builder.Default private List<TextModelPrice> items = new ArrayList<>();
}