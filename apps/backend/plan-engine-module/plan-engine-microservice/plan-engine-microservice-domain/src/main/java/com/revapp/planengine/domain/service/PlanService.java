package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanService {
    List<Plan> getAll();
    Optional<Plan> getById(UUID id);
    Plan save(Plan plan);
    Optional<Plan> update(UUID id, Plan plan);
    void delete(UUID id);
    PageResult<Plan> getByUser(UUID userId, boolean activeOnly, PageRequest page);
}
