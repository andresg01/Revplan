package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.PriceUnitEnum;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCatalogImageGenerationCategory {
    private PriceUnitEnum unit;
    @Builder.Default private List<ImageGenerationItem> items = new ArrayList<>();
}
