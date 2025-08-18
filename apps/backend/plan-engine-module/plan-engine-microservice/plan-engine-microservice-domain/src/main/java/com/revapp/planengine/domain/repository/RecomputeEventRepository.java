package com.revapp.planengine.domain.repository;

import com.revapp.planengine.domain.model.RecomputeEvent;

import java.util.List;
import java.util.UUID;

public interface RecomputeEventRepository {
    RecomputeEvent save(RecomputeEvent event);
    List<RecomputeEvent> findByPlanId(UUID planId);
}
