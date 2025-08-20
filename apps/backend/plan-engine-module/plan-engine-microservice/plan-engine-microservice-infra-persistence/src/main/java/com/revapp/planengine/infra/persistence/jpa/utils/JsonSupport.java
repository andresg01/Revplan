/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/
package com.revapp.planengine.infra.persistence.jpa.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapp.planengine.domain.model.*;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonSupport {
    private final ObjectMapper om;
    public JsonSupport(ObjectMapper om) { this.om = om; }

    /* =================== Genérico =================== */
    public <T> T to(Map<String,Object> m, Class<T> type) {
        return m == null ? null : om.convertValue(m, type);
    }

    @Named("objToMap")
    public Map<String,Object> toMap(Object v){
        return v == null ? null : om.convertValue(v, new TypeReference<Map<String,Object>>(){});
    }

    /* =================== params → PlanAdjustments (DB → dominio) =================== */
    @Named("mapToPlanAdjustments")
    public PlanAdjustments mapToPlanAdjustments(Map<String,Object> src){
        if (src == null) return null;
        Map<String,Object> m = new HashMap<>();

        // aceptar snake y camel
        Double savingPct = asDouble(
                src.get("saving_pct"),
                src.get("savingPct")
        );
        Integer emergencyMonths = asIntFirst(
                src.get("emergency_months"),
                src.get("emergencyMonths")
        );
        Object envelopes = src.get("envelopes"); // ya es map

        if (savingPct != null)       m.put("savingPct", savingPct);
        if (emergencyMonths != null) m.put("emergencyMonths", emergencyMonths);
        if (envelopes != null)       m.put("envelopes", envelopes);

        return om.convertValue(m, PlanAdjustments.class);
    }

    /* =================== PlanAdjustments → params (dominio → DB) =================== */
    @Named("adjustmentsToParams")
    public Map<String,Object> adjustmentsToParams(PlanAdjustments a){
        Map<String,Object> m = new HashMap<>();
        if (a == null) return m;
        if (a.getSavingPct() != null)        m.put("saving_pct", a.getSavingPct());
        if (a.getEmergencyMonths() != null)  m.put("emergency_months", a.getEmergencyMonths());
        if (a.getEnvelopes() != null)        m.put("envelopes", a.getEnvelopes());
        return m;
    }

    /* =================== kpis → PlanKPIs (DB → dominio) =================== */
    @Named("mapToPlanKPIs")
    public PlanKPIs mapToPlanKPIs(Map<String,Object> src){
        if (src == null) return null;
        Map<String,Object> m = new HashMap<>();

        // aceptar inglés camel, snake y seeds en español
        Double savingRate       = asDouble(src.get("savingRate"), src.get("saving_rate"), src.get("tasa_ahorro"));
        Double runwayMonths     = asDouble(src.get("runwayMonths"), src.get("runway_months"), src.get("runway_meses"));
        Double budgetCompliance = asDouble(src.get("budgetCompliance"), src.get("budget_compliance"), src.get("cumplimiento_global"));

        if (savingRate != null)       m.put("savingRate", savingRate);
        if (runwayMonths != null)     m.put("runwayMonths", runwayMonths);
        if (budgetCompliance != null) m.put("budgetCompliance", budgetCompliance);

        return om.convertValue(m, PlanKPIs.class);
    }

    /* =================== PlanKPIs → kpis (dominio → DB) =================== */
    @Named("kpisToDb")
    public Map<String,Object> kpisToDb(PlanKPIs k){
        Map<String,Object> m = new HashMap<>();
        if (k == null) return m;
        // guardamos en camelCase (no hay trigger sobre claves de KPIs)
        if (k.getSavingRate() != null)        m.put("savingRate", k.getSavingRate());
        if (k.getRunwayMonths() != null)      m.put("runwayMonths", k.getRunwayMonths());
        if (k.getBudgetCompliance() != null)  m.put("budgetCompliance", k.getBudgetCompliance());
        return m;
    }

    /* =================== Constraints (DB → dominio) =================== */
    @Named("mapToTemplateConstraints")
    public TemplateConstraints toTemplateConstraints(Map<String,Object> src){
        if (src == null) return null;
        Map<String,Object> norm = new HashMap<>();

        Object range = src.get("saving_pct_range");
        if (range instanceof List<?> l && l.size() == 2) {
            norm.put("minSavingPct", asBD(l.get(0), null));
            norm.put("maxSavingPct", asBD(l.get(1), null));
        } else {
            norm.put("minSavingPct", asBD(src.get("minSavingPct"), null));
            norm.put("maxSavingPct", asBD(src.get("maxSavingPct"), null));
        }

        Object minEm = src.get("min_emergency_months");
        Object maxEm = src.get("max_emergency_months");
        if (minEm == null) minEm = src.get("minEmergencyMonths");
        if (maxEm == null) maxEm = src.get("maxEmergencyMonths");
        if (minEm != null) norm.put("minEmergencyMonths", asInt(minEm, null));
        if (maxEm != null) norm.put("maxEmergencyMonths", asInt(maxEm, null));

        return om.convertValue(norm, TemplateConstraints.class);
    }

    /* =================== Weights (DB → dominio) =================== */
    @Named("mapToTemplateWeights")
    public TemplateWeights toTemplateWeights(Map<String,Object> src){
        if (src == null) return null;
        Map<String,Object> norm = new HashMap<>(src);
        if (!norm.containsKey("savingsGap") && norm.containsKey("gap")) {
            norm.put("savingsGap", norm.get("gap"));
        }
        return om.convertValue(norm, TemplateWeights.class);
    }

    /* =================== Defaults derivados (DB → dominio) =================== */
    @Named("deriveDefaultsFromConstraintsDoc")
    public TemplateDefaults deriveDefaultsFromConstraintsDoc(Map<String, Object> constraintsDoc) {
        if (constraintsDoc == null) return null;

        Map<String,Object> m = new HashMap<>();
        BigDecimal savingPct = null;
        Object range = constraintsDoc.get("saving_pct_range");
        if (range instanceof List<?> l && l.size() == 2) {
            BigDecimal a = asBD(l.get(0), null), b = asBD(l.get(1), null);
            if (a != null && b != null) savingPct = a.add(b).divide(BigDecimal.valueOf(2));
        }
        if (savingPct != null) m.put("savingPct", savingPct);

        Integer minEm = asInt(constraintsDoc.get("min_emergency_months"), null);
        Integer maxEm = asInt(constraintsDoc.get("max_emergency_months"), null);
        if (minEm != null && maxEm != null) m.put("emergencyMonths", Math.round((minEm + maxEm) / 2f));

        Object ts = constraintsDoc.get("target_split");
        if (ts instanceof Map<?, ?> tm) {
            Map<String, BigDecimal> env = new HashMap<>();
            for (Map.Entry<?,?> e : tm.entrySet()) {
                env.put(String.valueOf(e.getKey()), asBD(e.getValue(), BigDecimal.ZERO));
            }
            m.put("envelopes", env);
        }
        return m.isEmpty() ? null : om.convertValue(m, TemplateDefaults.class);
    }

    /* =================== constraints (dominio → DB) =================== */
    @Named("constraintsToDoc")
    public Map<String,Object> constraintsToDoc(TemplateConstraints c){
        if (c == null) return new HashMap<>();
        Map<String,Object> m = new HashMap<>();
        if (c.getMinSavingPct() != null && c.getMaxSavingPct() != null) {
            m.put("saving_pct_range", List.of(c.getMinSavingPct(), c.getMaxSavingPct()));
        } else {
            if (c.getMinSavingPct() != null) m.put("minSavingPct", c.getMinSavingPct());
            if (c.getMaxSavingPct() != null) m.put("maxSavingPct", c.getMaxSavingPct());
        }
        if (c.getMinEmergencyMonths() != null) m.put("min_emergency_months", c.getMinEmergencyMonths());
        if (c.getMaxEmergencyMonths() != null) m.put("max_emergency_months", c.getMaxEmergencyMonths());
        return m;
    }

    /* =================== weights (dominio → DB) =================== */
    @Named("weightsToMap")
    public Map<String,Object> weightsToMap(TemplateWeights w){
        if (w == null) return new HashMap<>();
        Map<String,Object> m = new HashMap<>();
        m.put("risk", w.getRisk());
        m.put("liquidity", w.getLiquidity());
        m.put("debt", w.getDebt());
        m.put("gap", w.getSavingsGap()); // clave de DB
        m.put("age", w.getAge());
        m.put("stability", w.getStability());
        return m;
    }

    /* =================== helpers =================== */
    private BigDecimal asBD(Object o, BigDecimal def){
        if (o == null) return def;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    private Integer asInt(Object o, Integer def){
        if (o == null) return def;
        try { return Integer.valueOf(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    // devuelve el primer Integer parseable de la lista
    private Integer asIntFirst(Object... candidates) {
        for (Object c : candidates) if (c != null) {
            if (c instanceof Number n) return n.intValue();
            try { return Integer.valueOf(String.valueOf(c)); } catch (Exception ignored) {}
        }
        return null;
    }
    private Double asDouble(Object... candidates) {
        for (Object c : candidates) if (c != null) {
            if (c instanceof Number n) return n.doubleValue();
            try { return Double.valueOf(String.valueOf(c)); } catch (Exception ignored) {}
        }
        return null;
    }
}
