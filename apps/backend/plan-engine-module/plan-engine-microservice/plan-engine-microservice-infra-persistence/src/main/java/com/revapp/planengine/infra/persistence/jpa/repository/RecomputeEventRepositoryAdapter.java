package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.infra.persistence.jpa.entities.RecomputeEventEntity;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaRecomputeEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RecomputeEventRepositoryAdapter implements RecomputeEventRepository {

    private final SpringDataRecomputeEventJpaRepository jpa;
    private final JpaRecomputeEventMapper mapper;
    private final SpringDataPlanJpaRepository planJpa;

    @Override
    public RecomputeEvent save(RecomputeEvent event) {
        log.debug("RecomputeEventRepo.save(planId={}, reason={}, hasPayload={})",
                event != null ? event.getPlanId() : null,
                event != null ? event.getReason() : null,
                event != null && event.getPayload() != null);

        // Lightweight guards (adapter boundary)
        if (event == null) {
            log.warn("RecomputeEventRepo.save -> event is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "event is required");
        }
        if (event.getPlanId() == null) {
            log.warn("RecomputeEventRepo.save -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (event.getReason() == null) {
            log.warn("RecomputeEventRepo.save -> reason is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "reason is required");
        }
        if (event.getPayload() == null) {
            log.debug("RecomputeEventRepo.save(planId={}) -> normalizing null payload to {}", event.getPlanId(), "{}");
            event.setPayload(Map.of());
        }

        // Fail fast with 404 if plan doesn't exist (better than FK late failure)
        var plan = planJpa.findById(event.getPlanId())
                .orElseThrow(() -> {
                    log.warn("RecomputeEventRepo.save -> plan not found: {}", event.getPlanId());
                    return new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + event.getPlanId());
                });

        try {
            Instant t0 = Instant.now();
            RecomputeEventEntity toSave = mapper.toEntity(event);
            toSave.setPlan(plan);
            RecomputeEventEntity saved = jpa.save(toSave);
            log.info("RecomputeEventRepo.save(planId={}, reason={}) -> saved entity id={} in {}",
                    event.getPlanId(), event.getReason(), saved.getId(), Duration.between(t0, Instant.now()));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("RecomputeEventRepo.save(planId={}) -> integrity violation: {}", event.getPlanId(), cause);
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving recompute event for plan " + event.getPlanId() + ": " + cause
            );
        }
    }

    @Override
    public List<RecomputeEvent> findByPlanId(UUID planId) {
        log.debug("RecomputeEventRepo.findByPlanId(planId={})", planId);
        if (planId == null) {
            log.warn("RecomputeEventRepo.findByPlanId -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (!planJpa.existsById(planId)) {
            log.warn("RecomputeEventRepo.findByPlanId -> plan not found: {}", planId);
            throw new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planId);
        }

        Instant t0 = Instant.now();
        List<RecomputeEvent> out = jpa.findByPlan_IdOrderByCreatedAtDesc(planId)
                .stream()
                .map(mapper::toDomain)
                .toList();
        log.info("RecomputeEventRepo.findByPlanId(planId={}) -> {} events in {}",
                planId, out.size(), Duration.between(t0, Instant.now()));
        return out;
    }
}
