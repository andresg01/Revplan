package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.domain.service.PlanRecomputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanRecomputeServiceImpl implements PlanRecomputeService {

    private final RecomputeEventRepository recomputeEventRepository;
    private final PlanRepository planRepository;

    @Override
    public UUID enqueueRecompute(UUID planId, RecomputeReasonEnum reason) {
        log.debug("PlanRecompute.enqueueRecompute(planId={}, reason={})", planId, reason);
        if (planId == null) {
            log.warn("PlanRecompute.enqueueRecompute -> planId is null");
            throw new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: null");
        }
        if (reason == null) {
            log.warn("PlanRecompute.enqueueRecompute -> reason is null");
            // usamos 422 si prefieres, pero mantener 400/required también es válido
            throw new NotFoundException(ErrorCode.MISSING_PARAMETER, "reason is required");
        }

        // Validación de existencia (404 coherente con el resto de adapters/servicios)
        planRepository.findById(planId)
                .orElseThrow(() -> {
                    log.warn("PlanRecompute.enqueueRecompute -> plan not found: {}", planId);
                    return new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planId);
                });

        RecomputeEvent ev = new RecomputeEvent();
        ev.setPlanId(planId);
        ev.setReason(reason);
        ev.setPayload(Map.of());
        ev.setCreatedAt(LocalDateTime.now());

        RecomputeEvent saved = recomputeEventRepository.save(ev);
        log.info("PlanRecompute.enqueueRecompute(planId={}, reason={}) -> eventId={}", planId, reason, saved.getId());
        return saved.getId();
    }
}
