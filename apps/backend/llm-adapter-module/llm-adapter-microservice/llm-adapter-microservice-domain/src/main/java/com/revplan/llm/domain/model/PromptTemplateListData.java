package com.revplan.llm.domain.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PromptTemplateListData {
    @Builder.Default private List<PromptTemplate> data = new ArrayList<>();
    private Pagination pagination;
}
