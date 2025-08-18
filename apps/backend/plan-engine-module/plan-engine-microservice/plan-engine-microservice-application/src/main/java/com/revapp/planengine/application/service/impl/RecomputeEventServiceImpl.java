/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.domain.service.RecomputeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecomputeEventServiceImpl implements RecomputeEventService {

    private final RecomputeEventRepository repository;

    @Override
    public RecomputeEvent append(RecomputeEvent event) {
        // Validaciones de negocio (precondiciones del caso de uso)
        if (event == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "event is required");
        }
        if (event.getPlanId() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (event.getReason() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "reason is required");
        }
        // Normalizamos payload nulo a vacío
        if (event.getPayload() == null) {
            event.setPayload(Map.of());
        }
        return repository.save(event);
    }

    @Override
    public List<RecomputeEvent> findByPlanId(UUID planId) {
        if (planId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        return repository.findByPlanId(planId);
    }
}
