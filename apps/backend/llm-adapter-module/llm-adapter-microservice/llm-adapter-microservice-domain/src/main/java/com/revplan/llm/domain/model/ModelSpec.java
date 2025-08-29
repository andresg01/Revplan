package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.ModelModalityEnum;
import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.enums.ModelStatusEnum;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModelSpec {
    private String id;
    private ModelProviderEnum provider;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    @Builder.Default private List<ModelModalityEnum> modalities = new ArrayList<>();
    private ModelStatusEnum status;
}
