package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import java.time.LocalDateTime;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCatalog {
    private ProviderEnum provider;
    private TierEnum tier;
    private String currency;                // ISO-4217
    private LocalDateTime effectiveAt;
    private PriceCatalogCategories categories;
}
