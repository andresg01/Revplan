package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.enums.PlanStatusEnum;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanEntity;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlanRepositoryAdapter implements PlanRepository {

    private final SpringDataPlanJpaRepository jpa;
    private final JpaPlanMapper mapper;

    @Override
    public List<Plan> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Plan> findByUser(UUID userId, boolean activeOnly, PageRequest page) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        var pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        var res = activeOnly
                ? jpa.findByUserIdAndStatusIn(userId, java.util.List.of(PlanStatusEnum.ACTIVE), pageable)
                : jpa.findByUserId(userId, pageable);

        var items = res.getContent().stream().map(mapper::toDomain).toList();

        return new PageResult<>(items, page.offset(), page.limit(), res.getTotalElements());
    }

    @Override
    public Plan save(Plan plan) {
        if (plan == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "plan is required");
        }
        if (plan.getUserId() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "plan.userId is required");
        }
        try {
            PlanEntity toSave = mapper.toEntity(plan);
            // normaliza timestamps en persist
            if (toSave.getCreatedAt() == null) toSave.setCreatedAt(LocalDateTime.now());
            toSave.setUpdatedAt(LocalDateTime.now());
            PlanEntity saved = jpa.save(toSave);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            // UNIQUE(user_id), checks, etc.
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving plan for user " + plan.getUserId() + ": " + ex.getMostSpecificCause().getMessage()
            );
        }
    }

    @Override
    public void deleteById(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        jpa.deleteById(id);
    }

    @Override
    public void updateActiveVersion(UUID id, int version) {
        if (id == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        if (version < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "version must be >= 1");
        }
        jpa.findById(id).ifPresent(e -> {
            e.setActiveVersion(version);
            e.setUpdatedAt(LocalDateTime.now());
            jpa.save(e);
        });
    }
}
