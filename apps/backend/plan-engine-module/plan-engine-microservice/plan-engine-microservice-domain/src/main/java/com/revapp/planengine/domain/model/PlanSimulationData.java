package com.revapp.planengine.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlanSimulationData {
    private PlanKPIs base;
    private PlanSimulationScenario scenario;
}
