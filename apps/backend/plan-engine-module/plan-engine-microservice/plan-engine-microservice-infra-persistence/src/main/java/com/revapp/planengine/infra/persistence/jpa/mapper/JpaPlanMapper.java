package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanEntity;
import org.mapstruct.*;

/**
 * Mapper Plan <-> PlanEntity.
 * - INSERT: usa toEntity (no pisa campos server-managed).
 * - UPDATE: usa updateEntityFromDomain (mergea sólo no-nulos y NO toca PK ni activeVersion).
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = { JpaPlanVersionMapper.class },
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface JpaPlanMapper {

    // Entity → Domain
    Plan toDomain(PlanEntity entity);

    // Domain → Entity (INSERT)
    @InheritInverseConfiguration
    @Mapping(target = "versions",  ignore = true) // normalmente se persisten aparte
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PlanEntity toEntity(Plan domain);

    // Domain → Entity (UPDATE MERGE) — no tocar PK ni server-managed
    @Mappings({
            @Mapping(target = "id",            ignore = true),
            @Mapping(target = "versions",      ignore = true),
            @Mapping(target = "createdAt",     ignore = true),
            @Mapping(target = "updatedAt",     ignore = true),
            // MUY IMPORTANTE: no sobrescribir activeVersion desde el dominio
            @Mapping(target = "activeVersion", ignore = true)
            // Nota: status y userId se mergean si vienen no-nulos (gracias a NullValuePropertyMappingStrategy.IGNORE)
    })
    void updateEntityFromDomain(Plan domain, @MappingTarget PlanEntity entity);
}
