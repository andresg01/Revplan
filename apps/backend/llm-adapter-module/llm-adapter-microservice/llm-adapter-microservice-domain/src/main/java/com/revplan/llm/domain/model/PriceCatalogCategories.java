package com.revplan.llm.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCatalogCategories {
    private TokensCategory textTokens;
    private TokensCategory imageTokens;                             // opcional
    private TokensCategory audioTokens;                             // opcional
    @Builder.Default private List<FineTuningItem> fineTuning = new ArrayList<>();
    @Builder.Default private List<BuiltInToolItem> builtInTools = new ArrayList<>();
    @Builder.Default private List<TranscriptionSpeechItem> transcriptionSpeech = new ArrayList<>();
    private PriceCatalogImageGenerationCategory imageGeneration;    // opcional
    private PriceCatalogEmbeddingsCategory embeddings;              // opcional
    private PriceCatalogModerationCategory moderation;              // opcional
    private TokensCategory legacyTextModels;                        // opcional
}
