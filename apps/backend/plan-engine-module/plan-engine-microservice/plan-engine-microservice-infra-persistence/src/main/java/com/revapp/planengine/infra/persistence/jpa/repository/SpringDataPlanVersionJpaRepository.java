package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPlanVersionJpaRepository extends JpaRepository<PlanVersionEntity, PlanVersionId> {
    Optional<PlanVersionEntity> findTopById_PlanIdOrderById_VersionDesc(UUID planId);
}
