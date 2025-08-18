package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.domain.repository.PlanVersionRepository;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionId;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlanVersionRepositoryAdapter implements PlanVersionRepository {

    private final SpringDataPlanVersionJpaRepository versionJpa;
    private final SpringDataPlanJpaRepository planJpa;
    private final SpringDataPlanTemplateJpaRepository templateJpa;
    private final JpaPlanVersionMapper mapper;

    @Override
    public Optional<PlanVersion> findLatest(UUID planId) {
        if (planId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        return versionJpa.findTopById_PlanIdOrderById_VersionDesc(planId).map(mapper::toDomain);
    }

    @Override
    public PlanVersion save(UUID planId, PlanVersion version) {
        // Defensas de adapter (si alguien usa el puerto directamente)
        if (planId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (version == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "version is required");
        }
        if (version.getVersion() == null || version.getVersion() < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "version.number must be >= 1");
        }
        if (version.getTemplateId() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "templateId is required in PlanVersion");
        }

        // Existencia de agregados referenciados (404)
        PlanEntity plan = planJpa.findById(planId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planId));
        PlanTemplateEntity template = templateJpa.findById(version.getTemplateId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + version.getTemplateId()));

        try {
            PlanVersionEntity entity = mapper.toEntity(version);
            entity.setId(new PlanVersionId(planId, version.getVersion()));
            entity.setPlan(plan);
            entity.setTemplate(template);
            // normalizamos createdAt por si llega null desde dominio
            entity.setCreatedAt(entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt());

            PlanVersionEntity saved = versionJpa.save(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            // 409/422 según contrato — aquí lo modelamos como INTEGRITY_VIOLATION
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving plan version " + version.getVersion() + " for plan " + planId + ": " +
                            ex.getMostSpecificCause().getMessage()
            );
        }
    }
}
