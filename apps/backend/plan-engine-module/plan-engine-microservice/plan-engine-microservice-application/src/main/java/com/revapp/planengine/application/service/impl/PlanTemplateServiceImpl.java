package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.service.PlanTemplateService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlanTemplateServiceImpl implements PlanTemplateService {

    private final PlanTemplateRepository repository;

    @Override
    public PageResult<PlanTemplate> list(String nameLike, PageRequest page) {
        return repository.findAll(nameLike, page);
    }

    @Override
    public Optional<PlanTemplate> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public PlanTemplate create(PlanTemplate template) {
        return repository.save(template);
    }

    @Override
    public Optional<PlanTemplate> update(String id, PlanTemplate template) {
        return repository.findById(id).map(existing -> {
            template.setId(existing.getId());
            return repository.save(template);
        });
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}
