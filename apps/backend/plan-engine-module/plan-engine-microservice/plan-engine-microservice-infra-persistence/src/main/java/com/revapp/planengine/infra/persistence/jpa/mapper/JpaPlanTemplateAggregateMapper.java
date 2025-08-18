package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = { JsonSupport.class },
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface JpaPlanTemplateAggregateMapper {

    // Entity -> Domain
    @Mappings({
            @Mapping(target = "constraints", source = "constraintsDoc",
                    qualifiedByName = "mapToTemplateConstraints"),
            @Mapping(target = "weights",     source = "scoringWeights",
                    qualifiedByName = "mapToTemplateWeights"),
            @Mapping(target = "defaults",    source = "constraintsDoc",
                    qualifiedByName = "deriveDefaultsFromConstraintsDoc"),
            @Mapping(target = "description", ignore = true),
            @Mapping(target = "active",      ignore = true)
    })
    PlanTemplate toDomain(PlanTemplateEntity entity);

    // Domain -> Entity
    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "paramsSchema",      ignore = true),
            @Mapping(target = "protectedTemplate", ignore = true),
            @Mapping(target = "checksum",          ignore = true),
            @Mapping(target = "createdAt",         ignore = true),
            @Mapping(target = "updatedAt",         ignore = true),
            @Mapping(target = "constraintsDoc", source = "constraints",
                    qualifiedByName = "constraintsToDoc"),
            @Mapping(target = "scoringWeights", source = "weights",
                    qualifiedByName = "weightsToMap")
    })
    PlanTemplateEntity toEntity(PlanTemplate domain);
}
