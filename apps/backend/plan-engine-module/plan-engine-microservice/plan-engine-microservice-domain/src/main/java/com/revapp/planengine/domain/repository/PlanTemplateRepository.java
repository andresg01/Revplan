package com.revapp.planengine.domain.repository;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface PlanTemplateRepository {
    PageResult<PlanTemplate> findAll(String nameLike, PageRequest page);
    Optional<PlanTemplate> findById(String id);
    PlanTemplate save(PlanTemplate template);
    void deleteById(String id);
}
