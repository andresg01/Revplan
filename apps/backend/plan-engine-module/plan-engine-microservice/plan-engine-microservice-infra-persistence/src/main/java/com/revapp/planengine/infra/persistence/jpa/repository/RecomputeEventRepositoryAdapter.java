/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.infra.persistence.jpa.entities.RecomputeEventEntity;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaRecomputeEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RecomputeEventRepositoryAdapter implements RecomputeEventRepository {

    private final SpringDataRecomputeEventJpaRepository jpa;
    private final JpaRecomputeEventMapper mapper;
    private final SpringDataPlanJpaRepository planJpa;

    @Override
    public RecomputeEvent save(RecomputeEvent event) {
        // Defensas de adapter (por si viniera desde otro puerto/servicio)
        if (event == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "event is required");
        }
        if (event.getPlanId() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (event.getReason() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "reason is required");
        }
        // payload: permitir vacío; normalizamos a {} para evitar nulls en DB
        if (event.getPayload() == null) {
            event.setPayload(Map.of());
        }

        // 404 si el plan no existe (evita FK lazy fallando tarde)
        var plan = planJpa.findById(event.getPlanId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + event.getPlanId()));

        try {
            RecomputeEventEntity toSave = mapper.toEntity(event);
            toSave.setPlan(plan);
            RecomputeEventEntity saved = jpa.save(toSave);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            // Colisiones de integridad, valores fuera de rango, etc.
            throw new BusinessException(ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving recompute event for plan " + event.getPlanId() + ": " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Override
    public List<RecomputeEvent> findByPlanId(UUID planId) {
        if (planId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        // Decisión: si el plan no existe, devolvemos 404 (más expresivo que lista vacía)
        if (!planJpa.existsById(planId)) {
            throw new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planId);
        }
        return jpa.findByPlan_IdOrderByCreatedAtDesc(planId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
