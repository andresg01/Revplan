package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.PromptVariableTypeEnum;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PromptVariable {
    private String name;
    private PromptVariableTypeEnum type;
    @Builder.Default private Boolean required = true;
    private String description;
}
