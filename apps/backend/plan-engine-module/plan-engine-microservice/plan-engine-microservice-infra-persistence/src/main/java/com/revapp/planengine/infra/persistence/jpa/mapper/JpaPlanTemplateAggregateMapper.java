package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanTemplateEntity;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

/**
 * Mapper de agregados de PlanTemplate (Entity <-> Domain).
 *
 * - INSERT: {@link #toEntity(PlanTemplate)} deja que la DB/@PrePersist rellenen defaults.
 * - UPDATE (merge): {@link #updateEntityFromDomain(PlanTemplate, PlanTemplateEntity)} actualiza solo campos permitidos.
 */
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

    // =========================
    // Entity -> Domain
    // =========================
    @Mappings({
            @Mapping(target = "constraints", source = "constraintsDoc",
                    qualifiedByName = "mapToTemplateConstraints"),
            @Mapping(target = "weights",     source = "scoringWeights",
                    qualifiedByName = "mapToTemplateWeights"),
            @Mapping(target = "defaults",    source = "constraintsDoc",
                    qualifiedByName = "deriveDefaultsFromConstraintsDoc"),
            // No persistimos/servimos estos dos desde BD ahora mismo:
            @Mapping(target = "description", ignore = true),
            @Mapping(target = "active",      ignore = true)
    })
    PlanTemplate toDomain(PlanTemplateEntity entity);

    // =========================
    // Domain -> Entity (INSERT)
    // =========================
    @InheritInverseConfiguration
    @Mappings({
            // Gestionados por servidor/DB → se ignoran en INSERT (DB/PrePersist pone defaults)
            @Mapping(target = "paramsSchema",      ignore = true),
            @Mapping(target = "protectedTemplate", ignore = true),
            @Mapping(target = "checksum",          ignore = true),
            @Mapping(target = "createdAt",         ignore = true),
            @Mapping(target = "updatedAt",         ignore = true),

            // Transformaciones JSON
            @Mapping(target = "constraintsDoc", source = "constraints",
                    qualifiedByName = "constraintsToDoc"),
            @Mapping(target = "scoringWeights", source = "weights",
                    qualifiedByName = "weightsToMap")
    })
    PlanTemplateEntity toEntity(PlanTemplate domain);

    // =========================
    // Domain -> Entity (UPDATE MERGE)
    // =========================
    /**
     * Aplica cambios del dominio sobre la entidad existente, preservando:
     * - id (PK), version, paramsSchema, protectedTemplate, checksum, createdAt/updatedAt.
     * Solo se actualizan: name, constraintsDoc, scoringWeights (si vienen no-nulos).
     */
    @BeanMapping(ignoreByDefault = true) // evita sobrescribir campos no mapeados
    @Mappings({
            @Mapping(target = "name", source = "name"),

            // Transformaciones JSON (solo si vienen no nulas gracias a NullValuePropertyMappingStrategy.IGNORE)
            @Mapping(target = "constraintsDoc", source = "constraints",
                    qualifiedByName = "constraintsToDoc"),
            @Mapping(target = "scoringWeights", source = "weights",
                    qualifiedByName = "weightsToMap"),

            // Campos que NO se deben tocar en un update funcional
            @Mapping(target = "id",                ignore = true),
            @Mapping(target = "version",           ignore = true),
            @Mapping(target = "paramsSchema",      ignore = true),
            @Mapping(target = "protectedTemplate", ignore = true),
            @Mapping(target = "checksum",          ignore = true),
            @Mapping(target = "createdAt",         ignore = true),
            @Mapping(target = "updatedAt",         ignore = true)
    })
    void updateEntityFromDomain(PlanTemplate domain, @MappingTarget PlanTemplateEntity entity);
}
