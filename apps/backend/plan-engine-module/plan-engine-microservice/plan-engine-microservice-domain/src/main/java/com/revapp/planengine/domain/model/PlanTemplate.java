package com.revapp.planengine.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTemplate {
    private String id;
    private String name;
    private String description; // opcional
    private TemplateDefaults defaults;
    private TemplateConstraints constraints;              // opcional
    private TemplateApplicabilityRules applicabilityRules;// opcional
    private TemplateWeights weights;                      // opcional
    @Builder.Default
    private Boolean active = true;
}
