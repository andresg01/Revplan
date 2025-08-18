package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanSimulationData;

import java.util.UUID;

public interface PlanSimulationService {
    PlanSimulationData simulate(UUID planId, PlanAdjustments adjustments);
}
