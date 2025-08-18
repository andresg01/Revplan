package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.infra.persistence.jpa.entities.RecomputeEventEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface JpaRecomputeEventMapper {

    // Entity → Domain
    @Mappings({
            @Mapping(target = "planId", source = "plan.id")
    })
    RecomputeEvent toDomain(RecomputeEventEntity entity);

    // Domain → Entity
    // No referenciamos createdAt/updatedAt porque no existen en el Entity (o en su Builder).
    @Mappings({
            @Mapping(target = "plan", ignore = true) // el servicio asociará el PlanEntity
    })
    RecomputeEventEntity toEntity(RecomputeEvent domain);
}
