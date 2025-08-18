package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.domain.enums.PlanStatusEnum;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface SpringDataPlanJpaRepository extends JpaRepository<PlanEntity, UUID> {

    Page<PlanEntity> findByUserId(UUID userId, Pageable pageable);

    Page<PlanEntity> findByUserIdAndStatusIn(UUID userId,
                                             Collection<PlanStatusEnum> statuses,
                                             Pageable pageable);
}
