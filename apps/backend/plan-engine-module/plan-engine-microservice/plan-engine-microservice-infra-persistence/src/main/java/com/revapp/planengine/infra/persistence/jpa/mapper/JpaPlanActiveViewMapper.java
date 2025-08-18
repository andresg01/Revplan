package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.enums.PlanSourceEnum;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanActiveView;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = JsonSupport.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface JpaPlanActiveViewMapper {

    @Mappings({
            @Mapping(target = "id",          source = "planId"),
            @Mapping(target = "userId",      source = "userId"),
            @Mapping(target = "planVersion", source = "versionCreatedAt"),
            @Mapping(target = "status",      source = "status"),
            @Mapping(target = "templateId",  source = "templateId"),
            @Mapping(target = "adjustments", source = "params", qualifiedByName = "mapToPlanAdjustments"),
            @Mapping(target = "kpis",        source = "kpis",   qualifiedByName = "mapToPlanKPIs"),
            @Mapping(target = "rationale",   source = "rationale"),
            @Mapping(target = "alerts",      source = "alerts", qualifiedByName = "anyToStringList")
    })
    Plan toDomain(PlanActiveView view);

    @Named("anyToStringList")
    default java.util.List<String> anyToStringList(Object alerts) {
        if (alerts == null) return java.util.List.of();
        if (alerts instanceof java.util.List<?> l) {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (Object o : l) out.add(String.valueOf(o));
            return out;
        }
        return java.util.List.of(String.valueOf(alerts));
    }

    // Si necesitas mapear source textual de la vista -> enum de dominio:
    default PlanSourceEnum mapSource(String src) {
        if (src == null) return null;
        return "rules".equals(src) ? PlanSourceEnum.RULES : PlanSourceEnum.RULES_PLUS_GPT;
    }
}
