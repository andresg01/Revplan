// application/service/impl/PlanServiceImpl.java
package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.service.PlanService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository repository;
    private final PlanReadRepository readRepository;

    @Override
    public List<Plan> getAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Plan> getById(UUID id) {
        // devuelve activo hidratado si existe
        var active = readRepository.findActiveById(id);
        if (active.isPresent()) return active;
        return repository.findById(id);
    }

    @Override
    public Plan save(Plan plan) {
        return repository.save(plan);
    }

    @Override
    public PageResult<Plan> getByUser(UUID userId, boolean activeOnly, PageRequest page) {
        if (activeOnly) return readRepository.findActiveByUser(userId, page);
        return repository.findByUser(userId, false, page);
    }

    @Override
    public Optional<Plan> update(UUID id, Plan plan) {
        return repository.findById(id).map(existing -> repository.save(plan));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
