package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.domain.service.PlanRecomputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanRecomputeServiceImpl implements PlanRecomputeService {

    private final RecomputeEventRepository recomputeEventRepository;
    private final PlanRepository planRepository; // para validar existencia (opcional pero recomendable)

    @Override
    public UUID enqueueRecompute(UUID planId, RecomputeReasonEnum reason) {
        // Validación opcional de existencia del plan
        planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        RecomputeEvent ev = new RecomputeEvent();
        ev.setPlanId(planId);
        ev.setReason(reason);
        ev.setPayload(Map.of()); // payload vacío por ahora
        ev.setCreatedAt(LocalDateTime.now());

        RecomputeEvent saved = recomputeEventRepository.save(ev);
        return saved.getId();
    }
}
