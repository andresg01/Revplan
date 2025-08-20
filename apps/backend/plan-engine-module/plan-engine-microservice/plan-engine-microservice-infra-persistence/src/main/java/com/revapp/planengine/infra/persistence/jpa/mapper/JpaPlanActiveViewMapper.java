/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/
package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.AiMeta;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanActiveView;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

import java.util.Map;

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
            // Normaliza snake_case→camelCase (saving_pct→savingPct, etc.)
            @Mapping(target = "adjustments", source = "params", qualifiedByName = "mapToPlanAdjustments"),
            @Mapping(target = "kpis",        source = "kpis",   qualifiedByName = "mapToPlanKPIs"),
            @Mapping(target = "rationale",   source = "rationale"),
            @Mapping(target = "alerts",      source = "alerts", qualifiedByName = "anyToStringList"),
            // NEW: rehidrata aiMeta desde params.ai_meta si viene
            @Mapping(target = "aiMeta",      source = "params", qualifiedByName = "extractAiMeta")
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

    @Named("extractAiMeta")
    default AiMeta extractAiMeta(Map<String, Object> params) {
        if (params == null) return null;
        Object raw = params.get("ai_meta");
        if (!(raw instanceof Map<?,?> m)) return null;

        AiMeta.AiMetaBuilder b = AiMeta.builder();
        try {
            b.provider(valStr(m.get("provider")));
            b.model(valStr(m.get("model")));
            b.promptVersion(valStr(m.get("promptVersion")));
            b.temperature(valDouble(m.get("temperature")));
            b.tokensPrompt(valInt(m.get("tokensPrompt")));
            b.tokensOutput(valInt(m.get("tokensOutput")));
            b.latencyMs(valInt(m.get("latencyMs")));
            return b.build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String valStr(Object o){ return o == null ? null : String.valueOf(o); }
    private static Integer valInt(Object o){
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return null; }
    }
    private static Double valDouble(Object o){
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch(Exception e){ return null; }
    }
}
