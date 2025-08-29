package com.revplan.llm.domain.model;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCatalogModerationCategory {
    private boolean free;
    private String notes; // opcional
}
