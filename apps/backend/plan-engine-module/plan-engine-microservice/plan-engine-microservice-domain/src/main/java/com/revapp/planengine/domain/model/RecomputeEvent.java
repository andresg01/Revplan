package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class RecomputeEvent {
    private UUID id;
    private UUID planId;
    private RecomputeReasonEnum reason;
    private Map<String,Object> payload;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
