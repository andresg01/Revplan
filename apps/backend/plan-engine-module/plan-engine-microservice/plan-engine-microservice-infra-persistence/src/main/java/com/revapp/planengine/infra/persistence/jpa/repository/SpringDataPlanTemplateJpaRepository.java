package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataPlanTemplateJpaRepository extends JpaRepository<PlanTemplateEntity, String> {
    Page<PlanTemplateEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
