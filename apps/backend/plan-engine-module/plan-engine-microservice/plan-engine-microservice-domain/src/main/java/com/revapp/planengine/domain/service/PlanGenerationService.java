package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.GeneratePlanRequest;
import com.revapp.planengine.domain.model.Plan;

public interface PlanGenerationService {
    Plan generate(GeneratePlanRequest request);
}
