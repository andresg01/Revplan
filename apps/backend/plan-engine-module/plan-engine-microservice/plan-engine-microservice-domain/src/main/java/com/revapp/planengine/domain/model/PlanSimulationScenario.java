package com.revapp.planengine.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlanSimulationScenario {
    private PlanAdjustments changes; // opcional según uso
    private PlanKPIs kpis;           // KPIs resultantes de la simulación
}
