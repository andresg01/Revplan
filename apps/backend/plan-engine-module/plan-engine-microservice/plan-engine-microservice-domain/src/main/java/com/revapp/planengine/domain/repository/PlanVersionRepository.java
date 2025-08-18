package com.revapp.planengine.domain.repository;

import com.revapp.planengine.domain.model.PlanVersion;

import java.util.Optional;
import java.util.UUID;

public interface PlanVersionRepository {
    Optional<PlanVersion> findLatest(UUID planId);
    PlanVersion save(UUID planId, PlanVersion version); // version.templateId debe venir informado
}
