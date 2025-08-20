package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.PlanStatusEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {
    private UUID id;
    private UUID userId;
    private LocalDateTime planVersion;
    private PlanStatusEnum status;
    private String templateId;
    private PlanAdjustments adjustments;
    private String rationale; // opcional
    @Builder.Default
    private List<String> alerts = new ArrayList<>();
    private PlanKPIs kpis;
    private AiMeta aiMeta;
}
