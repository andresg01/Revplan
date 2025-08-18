// infra/persistence/jpa/mapper/JpaPlanVersionMapper.java
package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionId;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = JsonSupport.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface JpaPlanVersionMapper {

    // Entity → Domain
    @Mappings({
            @Mapping(target = "version",     source = "id.version"),
            @Mapping(target = "adjustments", source = "params", qualifiedByName = "mapToPlanAdjustments"),
            @Mapping(target = "kpis",        source = "kpis",   qualifiedByName = "mapToPlanKPIs"),
            @Mapping(target = "templateId",  source = "template.id"),
            @Mapping(target = "source",      source = "source"),
            @Mapping(target = "rationale",   source = "rationale"),
            @Mapping(target = "alerts",      source = "alerts"),
            @Mapping(target = "createdAt",   source = "createdAt")
    })
    PlanVersion toDomain(PlanVersionEntity entity);

    // Domain → Entity (plan y template se setean en el adapter)
    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "plan",        ignore = true),
            @Mapping(target = "template",    ignore = true),
            @Mapping(target = "id",          expression = "java(new PlanVersionId(null, domain.getVersion()))"),
            @Mapping(target = "params",      source = "adjustments", qualifiedByName = "objToMap"),
            @Mapping(target = "kpis",        source = "kpis",        qualifiedByName = "objToMap"),
            @Mapping(target = "createdAt",   ignore = true)
    })
    PlanVersionEntity toEntity(PlanVersion domain);

    @AfterMapping
    default void ensureId(@MappingTarget PlanVersionEntity entity, PlanVersion domain) {
        if (entity.getId() == null) {
            entity.setId(new PlanVersionId(null, domain.getVersion()));
        }
    }
}
