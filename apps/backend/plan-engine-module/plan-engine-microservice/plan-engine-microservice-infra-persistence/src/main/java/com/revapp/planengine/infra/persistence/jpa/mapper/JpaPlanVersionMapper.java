/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/
package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.revapp.planengine.domain.model.AiMeta;
import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionEntity;
import com.revapp.planengine.infra.persistence.jpa.entities.PlanVersionId;
import com.revapp.planengine.infra.persistence.jpa.utils.JsonSupport;
import org.mapstruct.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        uses = JsonSupport.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface JpaPlanVersionMapper {

    /* ========== Entity → Domain ========== */
    @Mappings({
            @Mapping(target = "version",     source = "id.version"),
            @Mapping(target = "adjustments", source = "params", qualifiedByName = "mapToPlanAdjustments"),
            @Mapping(target = "kpis",        source = "kpis",   qualifiedByName = "mapToPlanKPIs"),
            @Mapping(target = "templateId",  source = "template.id"),
            @Mapping(target = "source",      source = "source"),
            @Mapping(target = "rationale",   source = "rationale"),
            @Mapping(target = "alerts",      source = "alerts"),
            @Mapping(target = "createdAt",   source = "createdAt"),
            // NEW: rehidrata aiMeta desde params.ai_meta
            @Mapping(target = "aiMeta",      source = "params", qualifiedByName = "extractAiMetaFromParams")
    })
    PlanVersion toDomain(PlanVersionEntity entity);

    /* ========== Domain → Entity ========== */
    @InheritInverseConfiguration
    @Mappings({
            @Mapping(target = "plan",        ignore = true),
            @Mapping(target = "template",    ignore = true),
            @Mapping(target = "id",          expression = "java(new PlanVersionId(null, domain.getVersion()))"),
            // params en snake_case para triggers
            @Mapping(target = "params",      source = "adjustments", qualifiedByName = "adjustmentsToParams"),
            // kpis normalizados (camel)
            @Mapping(target = "kpis",        source = "kpis",        qualifiedByName = "kpisToDb"),
            @Mapping(target = "createdAt",   ignore = true)
    })
    PlanVersionEntity toEntity(PlanVersion domain);

    @AfterMapping
    default void ensureId(@MappingTarget PlanVersionEntity entity, PlanVersion domain) {
        if (entity.getId() == null) {
            entity.setId(new PlanVersionId(null, domain.getVersion()));
        }
    }

    /** NEW: inyecta ai_meta en entity.params (sin cambiar el esquema) */
    @AfterMapping
    default void injectAiMeta(@MappingTarget PlanVersionEntity entity, PlanVersion domain) {
        AiMeta meta = domain.getAiMeta();
        if (meta == null) return;

        Map<String, Object> params = entity.getParams();
        if (params == null) {
            params = new LinkedHashMap<>();
            entity.setParams(params);
        }
        Map<String, Object> ai = new LinkedHashMap<>();
        if (meta.getProvider() != null)      ai.put("provider", meta.getProvider());
        if (meta.getModel() != null)         ai.put("model", meta.getModel());
        if (meta.getPromptVersion() != null) ai.put("promptVersion", meta.getPromptVersion());
        if (meta.getTemperature() != null)   ai.put("temperature", meta.getTemperature());
        if (meta.getTokensPrompt() != null)  ai.put("tokensPrompt", meta.getTokensPrompt());
        if (meta.getTokensOutput() != null)  ai.put("tokensOutput", meta.getTokensOutput());
        if (meta.getLatencyMs() != null)     ai.put("latencyMs", meta.getLatencyMs());

        params.put("ai_meta", ai);
    }

    @Named("extractAiMetaFromParams")
    default AiMeta extractAiMetaFromParams(Map<String, Object> params) {
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
