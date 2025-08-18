package com.revapp.planengine.domain.repository;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface PlanReadRepository {
    Optional<Plan> findActiveById(UUID planId);
    PageResult<Plan> findActiveByUser(UUID userId, PageRequest page);
}