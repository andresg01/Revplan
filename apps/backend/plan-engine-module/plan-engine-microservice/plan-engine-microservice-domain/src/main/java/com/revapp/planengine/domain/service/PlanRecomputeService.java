package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.enums.RecomputeReasonEnum;

import java.util.UUID;

public interface PlanRecomputeService {
    UUID enqueueRecompute(UUID planId, RecomputeReasonEnum reason);
}
