package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanTemplateAggregateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlanTemplateRepositoryAdapter implements PlanTemplateRepository {

    private final SpringDataPlanTemplateJpaRepository jpa;
    private final JpaPlanTemplateAggregateMapper mapper;

    private static final String ID_PATTERN = "^[A-Z0-9_]+$";

    @Override
    public PageResult<PlanTemplate> findAll(String nameLike, PageRequest page) {
        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        Page<PlanTemplateEntity> res = (nameLike == null || nameLike.isBlank())
                ? jpa.findAll(pageable)
                : jpa.findByNameContainingIgnoreCase(nameLike.trim(), pageable);

        return new PageResult<>(
                res.getContent().stream().map(mapper::toDomain).toList(),
                page.offset(),
                page.limit(),
                res.getTotalElements()
        );
    }

    @Override
    public Optional<PlanTemplate> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public PlanTemplate save(PlanTemplate template) {
        // Defensas mínimas a nivel de adapter (formato id / nombre)
        if (template == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template is required");
        }
        if (template.getId() == null || template.getId().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "template.id is required");
        }
        if (!template.getId().matches(ID_PATTERN)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "template.id must match " + ID_PATTERN);
        }
        if (template.getName() == null || template.getName().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "template.name is required");
        }

        try {
            PlanTemplateEntity saved = jpa.save(mapper.toEntity(template));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            // ids duplicados, checks, not-null, etc.
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving template " + template.getId() + ": " + ex.getMostSpecificCause().getMessage()
            );
        }
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        jpa.deleteById(id);
    }
}
