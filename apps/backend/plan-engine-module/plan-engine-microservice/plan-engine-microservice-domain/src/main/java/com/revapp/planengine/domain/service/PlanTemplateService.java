package com.revapp.planengine.domain.service;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;

import java.util.Optional;

public interface PlanTemplateService {
    PageResult<PlanTemplate> list(String nameLike, PageRequest page);
    Optional<PlanTemplate> getById(String id);
    PlanTemplate create(PlanTemplate template);
    Optional<PlanTemplate> update(String id, PlanTemplate template);
    void delete(String id);
}
