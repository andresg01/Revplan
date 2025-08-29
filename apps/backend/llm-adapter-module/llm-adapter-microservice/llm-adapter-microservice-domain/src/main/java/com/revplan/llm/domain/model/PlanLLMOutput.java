package com.revplan.llm.domain.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLLMOutput {
    private String templateId;
    private PlanAdjustments adjustments;
    private String rationale;
    @Builder.Default private List<String> alerts = new ArrayList<>();
}
