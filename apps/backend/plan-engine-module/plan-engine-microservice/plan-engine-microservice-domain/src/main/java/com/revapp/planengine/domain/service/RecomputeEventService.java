package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.RecomputeEvent;

import java.util.List;
import java.util.UUID;

public interface RecomputeEventService {
    RecomputeEvent append(RecomputeEvent event);
    List<RecomputeEvent> findByPlanId(UUID planId);
}
