package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.PlanSourceEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlanVersion {
    private Integer version;
    private String templateId;            // EQUILIBRADO_50_30_20
    private PlanAdjustments adjustments;
    private PlanKPIs kpis;
    private String rationale;
    private List<String> alerts;
    private PlanSourceEnum source;        // RULES | RULES_GPT
    private LocalDateTime createdAt;
}
