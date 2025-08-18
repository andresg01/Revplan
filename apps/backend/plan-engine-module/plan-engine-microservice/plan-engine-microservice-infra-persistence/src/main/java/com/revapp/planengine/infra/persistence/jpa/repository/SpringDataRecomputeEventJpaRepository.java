package com.revapp.planengine.infra.persistence.jpa.repository;

import com.revapp.planengine.infra.persistence.jpa.entities.RecomputeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataRecomputeEventJpaRepository extends JpaRepository<RecomputeEventEntity, UUID> {
    List<RecomputeEventEntity> findByPlan_IdOrderByCreatedAtDesc(UUID planId);
}
