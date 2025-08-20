package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.service.PlanService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanServiceImpl implements PlanService {

    private final PlanRepository repository;
    private final PlanReadRepository readRepository;

    @Override
    public List<Plan> getAll() {
        log.debug("PlanService.getAll() -> fetching all plans");
        Instant t0 = Instant.now();
        List<Plan> res = repository.findAll();
        log.info("PlanService.getAll() -> fetched {} plans in {}", res.size(), Duration.between(t0, Instant.now()));
        return res;
    }

    @Override
    public Optional<Plan> getById(UUID id) {
        log.debug("PlanService.getById(id={}) -> fetching active view first", id);
        Instant t0 = Instant.now();

        // devuelve activo hidratado si existe
        var active = readRepository.findActiveById(id);
        if (active.isPresent()) {
            log.info("PlanService.getById(id={}) -> active view found in {}", id, Duration.between(t0, Instant.now()));
            return active;
        }

        log.debug("PlanService.getById(id={}) -> active view not found, falling back to repository", id);
        var res = repository.findById(id);
        log.info("PlanService.getById(id={}) -> fallback result present? {} (took {})",
                id, res.isPresent(), Duration.between(t0, Instant.now()));
        return res;
    }

    @Override
    public Plan save(Plan plan) {
        log.info("PlanService.save(userId={}, planId={}) -> saving plan",
                plan != null ? plan.getUserId() : null,
                plan != null ? plan.getId() : null);
        Instant t0 = Instant.now();
        Plan saved = repository.save(plan);
        log.info("PlanService.save(planId={}, userId={}) -> saved in {}",
                saved.getId(), saved.getUserId(), Duration.between(t0, Instant.now()));
        return saved;
    }

    @Override
    public PageResult<Plan> getByUser(UUID userId, boolean activeOnly, PageRequest page) {
        log.debug("PlanService.getByUser(userId={}, activeOnly={}, offset={}, limit={})",
                userId, activeOnly, page != null ? page.offset() : null, page != null ? page.limit() : null);
        Instant t0 = Instant.now();

        PageResult<Plan> result = activeOnly
                ? readRepository.findActiveByUser(userId, page)
                : repository.findByUser(userId, false, page);

        log.info("PlanService.getByUser(userId={}, activeOnly={}) -> {} items (total={}) in {}",
                userId, activeOnly, result.items().size(), result.total(), Duration.between(t0, Instant.now()));
        return result;
    }

    @Override
    public Optional<Plan> update(UUID id, Plan plan) {
        log.info("PlanService.update(id={}, incomingPlanId={}, userId={})",
                id, plan != null ? plan.getId() : null, plan != null ? plan.getUserId() : null);
        Instant t0 = Instant.now();

        Optional<Plan> updated = repository.findById(id).map(existing -> {
            log.debug("PlanService.update(id={}) -> existing found, delegating to repository.save()", id);
            return repository.save(plan);
        });

        log.info("PlanService.update(id={}) -> updated? {} (took {})",
                id, updated.isPresent(), Duration.between(t0, Instant.now()));
        return updated;
    }

    @Override
    public void delete(UUID id) {
        log.warn("PlanService.delete(id={}) -> deleting plan", id);
        Instant t0 = Instant.now();
        repository.deleteById(id);
        log.info("PlanService.delete(id={}) -> deleted in {}", id, Duration.between(t0, Instant.now()));
    }
}
