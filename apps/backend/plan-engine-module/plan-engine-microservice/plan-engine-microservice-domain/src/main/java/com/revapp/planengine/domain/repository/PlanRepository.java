package com.revapp.planengine.domain.repository;


import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {
    List<Plan> findAll();
    Optional<Plan> findById(UUID id);
    Plan save(Plan plan);
    void deleteById(UUID id);
    PageResult<Plan> findByUser(UUID userId, boolean activeOnly, PageRequest page);
    void updateActiveVersion(UUID planId, int version);

}
