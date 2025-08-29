package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.PriceUnitEnum;
import java.util.ArrayList;
import java.util.List;

import com.revplan.llm.domain.model.EmbeddingItem;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCatalogEmbeddingsCategory {
    private PriceUnitEnum unit;
    @Builder.Default private List<EmbeddingItem> items = new ArrayList<>();
}
