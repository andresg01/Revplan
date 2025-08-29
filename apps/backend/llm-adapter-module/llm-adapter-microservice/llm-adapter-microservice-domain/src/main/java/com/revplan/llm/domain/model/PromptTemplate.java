package com.revplan.llm.domain.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PromptTemplate {
    private String id;
    private String name;
    private String systemPrompt;
    private String userTemplate;
    @Builder.Default private List<PromptVariable> variables = new ArrayList<>();
    /** El DTO es _protected; aquí lo nombramos de forma clara para MapStruct. */
    @Builder.Default private Boolean protectedFlag = false;
}
