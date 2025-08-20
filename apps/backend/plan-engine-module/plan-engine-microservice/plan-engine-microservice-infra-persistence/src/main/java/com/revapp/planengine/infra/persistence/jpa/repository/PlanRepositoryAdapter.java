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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PlanRepositoryAdapter implements PlanRepository {

    private final SpringDataPlanJpaRepository jpa;
    private final JpaPlanMapper mapper;

    @Override
    public List<Plan> findAll() {
        log.debug("PlanRepository.findAll() -> query all");
        Instant t0 = Instant.now();
        List<Plan> res = jpa.findAll().stream().map(mapper::toDomain).toList();
        log.info("PlanRepository.findAll() -> {} rows in {}", res.size(), Duration.between(t0, Instant.now()));
        return res;
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        log.debug("PlanRepository.findById(id={})", id);
        if (id == null) {
            log.warn("PlanRepository.findById -> missing id");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        Optional<Plan> res = jpa.findById(id).map(mapper::toDomain);
        log.info("PlanRepository.findById(id={}) -> present? {} (took {})",
                id, res.isPresent(), Duration.between(t0, Instant.now()));
        return res;
    }

    @Override
    public PageResult<Plan> findByUser(UUID userId, boolean activeOnly, PageRequest page) {
        log.debug("PlanRepository.findByUser(userId={}, activeOnly={}, offset={}, limit={})",
                userId, activeOnly, page != null ? page.offset() : null, page != null ? page.limit() : null);
        if (userId == null) {
            log.warn("PlanRepository.findByUser -> missing userId");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        if (page == null) {
            log.warn("PlanRepository.findByUser -> missing page");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "page is required");
        }

        Instant t0 = Instant.now();
        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        var pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        var res = activeOnly
                ? jpa.findByUserIdAndStatusIn(userId, java.util.List.of(PlanStatusEnum.ACTIVE), pageable)
                : jpa.findByUserId(userId, pageable);

        var items = res.getContent().stream().map(mapper::toDomain).toList();

        log.info("PlanRepository.findByUser(userId={}, activeOnly={}) -> page {} of {}, {} items (total={}) in {}",
                userId, activeOnly, pageIndex, res.getTotalPages(), items.size(), res.getTotalElements(),
                Duration.between(t0, Instant.now()));

        return new PageResult<>(items, page.offset(), page.limit(), res.getTotalElements());
    }

    @Override
    public Plan save(Plan plan) {
        log.debug("PlanRepository.save(planId={}, userId={})",
                plan != null ? plan.getId() : null,
                plan != null ? plan.getUserId() : null);

        if (plan == null) {
            log.warn("PlanRepository.save -> plan is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "plan is required");
        }
        if (plan.getUserId() == null) {
            log.warn("PlanRepository.save -> plan.userId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "plan.userId is required");
        }

        try {
            Instant t0 = Instant.now();
            final PlanEntity saved;

            // UPDATE
            if (plan.getId() != null && jpa.existsById(plan.getId())) {
                PlanEntity existing = jpa.findById(plan.getId()).orElseThrow();
                Integer beforeActive = existing.getActiveVersion();
                var beforeStatus = existing.getStatus();

                mapper.updateEntityFromDomain(plan, existing);

                if (existing.getCreatedAt() == null) {
                    existing.setCreatedAt(LocalDateTime.now());
                }
                existing.setUpdatedAt(LocalDateTime.now());

                saved = jpa.save(existing);

                log.info("PlanRepository.save -> UPDATE (planId={}), active {} -> {}, status {} -> {}, took {}",
                        saved.getId(), beforeActive, saved.getActiveVersion(), beforeStatus, saved.getStatus(),
                        Duration.between(t0, Instant.now()));
            } else {
                // INSERT
                var toSave = mapper.toEntity(plan);
                if (toSave.getActiveVersion() == null) toSave.setActiveVersion(0);
                if (toSave.getStatus() == null)        toSave.setStatus(PlanStatusEnum.ACTIVE);
                if (toSave.getCreatedAt() == null)     toSave.setCreatedAt(LocalDateTime.now());
                toSave.setUpdatedAt(LocalDateTime.now());

                saved = jpa.save(toSave);

                log.info("PlanRepository.save -> INSERT (planId={}, userId={}, activeVersion={}, status={}) in {}",
                        saved.getId(), saved.getUserId(), saved.getActiveVersion(), saved.getStatus(),
                        Duration.between(t0, Instant.now()));
            }
            return mapper.toDomain(saved);

        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            log.warn("PlanRepository.save -> integrity violation for userId={}, cause={}", plan.getUserId(), cause);
            throw new BusinessException(
                    ErrorCode.INTEGRITY_VIOLATION,
                    "Constraint violation saving plan for user " + plan.getUserId() + ": " + cause
            );
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("PlanRepository.save -> unexpected error", ex);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unexpected error saving plan: " + ex.getMessage()
            );
        }
    }

    @Override
    public void deleteById(UUID id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        jpa.deleteById(id);
        log.info("PlanRepository.deleteById(id={}) -> deleted in {}", id, Duration.between(t0, Instant.now()));
    }

    @Override
    public void updateActiveVersion(UUID id, int version) {
        if (id == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        if (version < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "version must be >= 1");
        }
        Instant t0 = Instant.now();
        jpa.findById(id).ifPresent(e -> {
            Integer before = e.getActiveVersion();
            e.setActiveVersion(version);
            e.setUpdatedAt(LocalDateTime.now());
            jpa.save(e);
            log.info("PlanRepository.updateActiveVersion(id={}) -> {} -> {} in {}",
                    id, before, version, Duration.between(t0, Instant.now()));
        });
    }
}
