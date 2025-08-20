package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanActiveViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PlanReadRepositoryAdapter implements PlanReadRepository {

    private final SpringDataPlanActiveViewJpaRepository jpa;
    private final JpaPlanActiveViewMapper mapper;

    @Override
    public Optional<Plan> findActiveById(UUID planId) {
        log.debug("PlanReadRepo.findActiveById(planId={})", planId);
        if (planId == null) {
            log.warn("PlanReadRepo.findActiveById -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        Instant t0 = Instant.now();
        Optional<Plan> out = jpa.findById(planId).map(mapper::toDomain);
        log.info("PlanReadRepo.findActiveById(planId={}) -> {} in {}",
                planId, out.isPresent() ? "FOUND" : "NOT_FOUND", Duration.between(t0, Instant.now()));
        return out;
    }

    @Override
    public PageResult<Plan> findActiveByUser(UUID userId, PageRequest page) {
        log.debug("PlanReadRepo.findActiveByUser(userId={}, page={})", userId, page);
        if (userId == null) {
            log.warn("PlanReadRepo.findActiveByUser -> userId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        if (page == null) {
            log.warn("PlanReadRepo.findActiveByUser -> page is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "page is required");
        }

        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        Instant t0 = Instant.now();
        var res = jpa.findByUserId(userId, pageable);
        var items = res.getContent().stream().map(mapper::toDomain).toList();
        PageResult<Plan> out = new PageResult<>(items, page.offset(), page.limit(), res.getTotalElements());

        log.info("PlanReadRepo.findActiveByUser(userId={}) -> {} items (total={}) in {}",
                userId, items.size(), out.total(), Duration.between(t0, Instant.now()));
        return out;
    }
}
