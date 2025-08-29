package com.revplan.llm.domain.model;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmbeddingItem {
    private String modelId;
    private EmbeddingPrice price;
}
