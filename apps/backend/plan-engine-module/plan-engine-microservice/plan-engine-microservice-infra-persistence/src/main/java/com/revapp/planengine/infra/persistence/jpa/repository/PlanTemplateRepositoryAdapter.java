package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanTemplateAggregateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PlanTemplateRepositoryAdapter implements PlanTemplateRepository {

    private final SpringDataPlanTemplateJpaRepository jpa;
    private final JpaPlanTemplateAggregateMapper mapper;

    @Override
    public PageResult<PlanTemplate> findAll(String nameLike, PageRequest page) {
        log.debug("PlanTemplateRepo.findAll(nameLike='{}', page={})", nameLike, page);
        if (page == null) {
            log.warn("PlanTemplateRepo.findAll -> page is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "page is required");
        }
        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        Instant t0 = Instant.now();
        Page<PlanTemplateEntity> res = (nameLike == null || nameLike.isBlank())
                ? jpa.findAll(pageable)
                : jpa.findByNameContainingIgnoreCase(nameLike.trim(), pageable);

        PageResult<PlanTemplate> out = new PageResult<>(
                res.getContent().stream().map(mapper::toDomain).toList(),
                page.offset(),
                page.limit(),
                res.getTotalElements()
        );
        log.info("PlanTemplateRepo.findAll(nameLike='{}') -> {} items (total={}) in {}",
                nameLike, out.items().size(), out.total(), Duration.between(t0, Instant.now()));
        return out;
    }

    @Override
    public Optional<PlanTemplate> findById(String id) {
        log.debug("PlanTemplateRepo.findById(id={})", id);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateRepo.findById -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        Optional<PlanTemplate> opt = jpa.findById(id).map(mapper::toDomain);
        log.info("PlanTemplateRepo.findById(id={}) -> {} in {}",
                id, opt.isPresent() ? "FOUND" : "NOT_FOUND", Duration.between(t0, Instant.now()));
        return opt;
    }

    /**
     * Save template:
     * - If exists → UPDATE (merge).
     * - If not → INSERT (DB/@PrePersist defaults).
     * 404/409 policy is decided in the service layer.
     */
    @Override
    public PlanTemplate save(PlanTemplate template) {
        log.debug("PlanTemplateRepo.save(id={}, name={})",
                template != null ? template.getId() : null,
                template != null ? template.getName() : null);

        if (template == null) {
            log.warn("PlanTemplateRepo.save -> template is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template is required");
        }
        if (template.getId() == null || template.getId().isBlank()) {
            log.warn("PlanTemplateRepo.save -> template.id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "template.id is required");
        }

        try {
            final PlanTemplateEntity saved;
            Instant t0 = Instant.now();
            if (jpa.existsById(template.getId())) {
                log.debug("PlanTemplateRepo.save -> UPDATE path for id={}", template.getId());
                var existing = jpa.findById(template.getId()).orElseThrow();
                mapper.updateEntityFromDomain(template, existing);
                saved = jpa.save(existing);
                log.info("PlanTemplateRepo.save(id={}) -> UPDATED in {}", template.getId(), Duration.between(t0, Instant.now()));
            } else {
                log.debug("PlanTemplateRepo.save -> INSERT path for id={}", template.getId());
                var entity = mapper.toEntity(template);
                saved = jpa.save(entity);
                log.info("PlanTemplateRepo.save(id={}) -> INSERTED in {}", template.getId(), Duration.between(t0, Instant.now()));
            }
            return mapper.toDomain(saved);

        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("PlanTemplateRepo.save(id={}) -> integrity violation: {}", template.getId(), cause);
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving template " + template.getId() + ": " + cause
            );
        } catch (RuntimeException ex) {
            log.error("PlanTemplateRepo.save(id={}) -> unexpected error", template.getId(), ex);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unexpected error saving template " + template.getId() + ": " + ex.getMessage()
            );
        }
    }

    @Override
    public void deleteById(String id) {
        log.debug("PlanTemplateRepo.deleteById(id={})", id);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateRepo.deleteById -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        if (!jpa.existsById(id)) {
            log.warn("PlanTemplateRepo.deleteById -> template not found: {}", id);
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + id);
        }
        try {
            Instant t0 = Instant.now();
            jpa.deleteById(id);
            log.info("PlanTemplateRepo.deleteById(id={}) -> DELETED in {}", id, Duration.between(t0, Instant.now()));
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("PlanTemplateRepo.deleteById(id={}) -> integrity violation: {}", id, cause);
            // e.g. FK in plan_version
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Cannot delete template " + id + ": " + cause
            );
        }
    }
}
