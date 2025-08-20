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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PlanVersionRepositoryAdapter implements PlanVersionRepository {

    private final SpringDataPlanVersionJpaRepository versionJpa;
    private final SpringDataPlanJpaRepository planJpa;
    private final SpringDataPlanTemplateJpaRepository templateJpa;
    private final JpaPlanVersionMapper mapper;

    @Override
    public Optional<PlanVersion> findLatest(UUID planId) {
        log.debug("PlanVersionRepo.findLatest(planId={})", planId);
        if (planId == null) {
            log.warn("PlanVersionRepo.findLatest -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        Instant t0 = Instant.now();
        Optional<PlanVersion> out = versionJpa
                .findTopById_PlanIdOrderById_VersionDesc(planId)
                .map(mapper::toDomain);
        log.info("PlanVersionRepo.findLatest(planId={}) -> {} in {}",
                planId, out.isPresent() ? "FOUND" : "NOT_FOUND", Duration.between(t0, Instant.now()));
        return out;
    }

    @Override
    public PlanVersion save(UUID planId, PlanVersion version) {
        log.debug("PlanVersionRepo.save(planId={}, versionNo={}, templateId={})",
                planId,
                version != null ? version.getVersion() : null,
                version != null ? version.getTemplateId() : null);

        // Adapter guards
        if (planId == null) {
            log.warn("PlanVersionRepo.save -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (version == null) {
            log.warn("PlanVersionRepo.save -> version is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "version is required");
        }
        if (version.getVersion() == null || version.getVersion() < 1) {
            log.warn("PlanVersionRepo.save -> invalid version number: {}", version.getVersion());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "version.number must be >= 1");
        }
        if (version.getTemplateId() == null) {
            log.warn("PlanVersionRepo.save -> templateId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "templateId is required in PlanVersion");
        }

        // Ensure referenced aggregates exist (404 early)
        PlanEntity plan = planJpa.findById(planId)
                .orElseThrow(() -> {
                    log.warn("PlanVersionRepo.save -> plan not found: {}", planId);
                    return new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planId);
                });
        PlanTemplateEntity template = templateJpa.findById(version.getTemplateId())
                .orElseThrow(() -> {
                    log.warn("PlanVersionRepo.save -> template not found: {}", version.getTemplateId());
                    return new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + version.getTemplateId());
                });

        try {
            Instant t0 = Instant.now();
            PlanVersionEntity entity = mapper.toEntity(version);
            entity.setId(new PlanVersionId(planId, version.getVersion()));
            entity.setPlan(plan);
            entity.setTemplate(template);
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }

            PlanVersionEntity saved = versionJpa.save(entity);
            log.info("PlanVersionRepo.save(planId={}, versionNo={}) -> SAVED in {}",
                    planId, version.getVersion(), Duration.between(t0, Instant.now()));
            return mapper.toDomain(saved);

        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("PlanVersionRepo.save(planId={}, versionNo={}) -> integrity violation: {}",
                    planId, version.getVersion(), cause);
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving plan version " + version.getVersion() + " for plan " + planId + ": " + cause
            );
        } catch (RuntimeException ex) {
            log.error("PlanVersionRepo.save(planId={}, versionNo={}) -> unexpected error", planId, version.getVersion(), ex);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unexpected error saving plan version " + version.getVersion() + " for plan " + planId + ": " + ex.getMessage()
            );
        }
    }
}
