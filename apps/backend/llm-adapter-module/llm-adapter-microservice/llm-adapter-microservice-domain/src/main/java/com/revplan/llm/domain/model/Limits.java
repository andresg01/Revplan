package com.revplan.llm.domain.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Limits {
    private Integer limitPerMinute;
    private Integer estimatedUsageMinute; // nullable
    private Integer burstAllowed;         // nullable
}
