package com.revplan.llm.domain.model;

import com.revplan.llm.domain.enums.ResponseFormatEnum;
import lombok.*;
import java.util.HashMap;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneratePlanJsonRequest {
    private String templateId;
    @Builder.Default private Map<String, Object> variables = new HashMap<>();
    private ResponseFormatEnum responseFormat;
    private Object schema; // si null => usar oficial del plan
    private GenerationParams generation;
}
