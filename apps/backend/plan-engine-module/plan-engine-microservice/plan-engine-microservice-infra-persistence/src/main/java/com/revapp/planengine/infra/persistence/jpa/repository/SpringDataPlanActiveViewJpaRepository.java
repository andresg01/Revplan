package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.infra.persistence.jpa.entities.PlanActiveView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataPlanActiveViewJpaRepository extends JpaRepository<PlanActiveView, UUID> {
    Page<PlanActiveView> findByUserId(UUID userId, Pageable pageable);
}
