package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.persistence.jpa.mapper.JpaPlanActiveViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlanReadRepositoryAdapter implements PlanReadRepository {

    private final SpringDataPlanActiveViewJpaRepository jpa;
    private final JpaPlanActiveViewMapper mapper;

    @Override
    public Optional<Plan> findActiveById(UUID planId) {
        if (planId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        return jpa.findById(planId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Plan> findActiveByUser(UUID userId, PageRequest page) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        int size = Math.max(1, page.limit());
        int pageIndex = (page.offset() <= 0 ? 0 : page.offset() / size);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageIndex, size);

        var res = jpa.findByUserId(userId, pageable);
        var items = res.getContent().stream().map(mapper::toDomain).toList();

        return new PageResult<>(items, page.offset(), page.limit(), res.getTotalElements());
    }
}
