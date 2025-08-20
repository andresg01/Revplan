package com.revapp.planengine.domain.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiOptions {
    private String provider;
    private String model;
    private Double temperature;
    private String promptVersion;
}
