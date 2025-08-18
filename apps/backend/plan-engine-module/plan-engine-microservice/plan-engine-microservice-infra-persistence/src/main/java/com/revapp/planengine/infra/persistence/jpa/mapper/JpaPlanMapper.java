package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

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

    // Domain → Entity
    @InheritInverseConfiguration
    @Mapping(target = "versions",  ignore = true) // normalmente se persisten aparte
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PlanEntity toEntity(Plan domain);
}
