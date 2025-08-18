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

    // ---------- Genérico
    public <T> T to(Map<String,Object> m, Class<T> type) {
        return m == null ? null : om.convertValue(m, type);
    }

    // ---------- Map -> POJO (usados por MapStruct)
    @Named("mapToPlanAdjustments")
    public PlanAdjustments toPlanAdjustments(Map<String,Object> m){
        return to(m, PlanAdjustments.class);
    }

    @Named("mapToPlanKPIs")
    public PlanKPIs toPlanKPIs(Map<String,Object> m){
        return to(m, PlanKPIs.class);
    }

    // === Constraints (DB -> dominio) con normalización de nombres
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

        return to(norm, TemplateConstraints.class);
    }

    // === Weights (DB -> dominio): gap -> savingsGap, BigDecimal
    @Named("mapToTemplateWeights")
    public TemplateWeights toTemplateWeights(Map<String,Object> src){
        if (src == null) return null;
        Map<String,Object> norm = new HashMap<>(src);
        if (!norm.containsKey("savingsGap") && norm.containsKey("gap")) {
            norm.put("savingsGap", norm.get("gap"));
        }
        // el resto de campos se mapean por nombre; Jackson convertirá a BigDecimal si tu POJO lo pide
        return to(norm, TemplateWeights.class);
    }

    // === Defaults derivados desde constraints_doc (DB -> dominio)
    @Named("deriveDefaultsFromConstraintsDoc")
    public TemplateDefaults deriveDefaultsFromConstraintsDoc(Map<String, Object> constraintsDoc) {
        if (constraintsDoc == null) return null;

        Map<String,Object> m = new HashMap<>();

        // savingPct = media del rango si existe
        BigDecimal savingPct = null;
        Object range = constraintsDoc.get("saving_pct_range");
        if (range instanceof List<?> l && l.size() == 2) {
            BigDecimal a = asBD(l.get(0), null), b = asBD(l.get(1), null);
            if (a != null && b != null) {
                savingPct = a.add(b).divide(BigDecimal.valueOf(2));
            }
        }
        if (savingPct != null) m.put("savingPct", savingPct);

        // emergencyMonths = media redondeada si hay min/max
        Integer minEm = asInt(constraintsDoc.get("min_emergency_months"), null);
        Integer maxEm = asInt(constraintsDoc.get("max_emergency_months"), null);
        if (minEm != null && maxEm != null) {
            m.put("emergencyMonths", Math.round((minEm + maxEm) / 2f));
        }

        // envelopes = target_split si existe (a BigDecimal)
        Object ts = constraintsDoc.get("target_split");
        if (ts instanceof Map<?, ?> tm) {
            Map<String, BigDecimal> env = new HashMap<>();
            for (Map.Entry<?,?> e : ((Map<?,?>) ts).entrySet()) {
                env.put(String.valueOf(e.getKey()), asBD(e.getValue(), BigDecimal.ZERO));
            }
            m.put("envelopes", env);
        }

        if (m.isEmpty()) return null;
        // Importante: usamos convertValue para evitar depender de builder/setters
        return om.convertValue(m, TemplateDefaults.class);
    }

    // ---------- POJO -> Map (jsonb) con traducciones

    // constraints (dominio) -> constraints_doc (DB)
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

    // weights (dominio) -> scoring_weights (DB) con savingsGap -> gap
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

    // genérico (lo mantienes por compatibilidad)
    @Named("objToMap")
    public Map<String,Object> toMap(Object v){
        return v == null ? null : om.convertValue(v, new TypeReference<Map<String,Object>>(){});
    }

    // ---- helpers (BigDecimal / Integer) ----
    private BigDecimal asBD(Object o, BigDecimal def){
        if (o == null) return def;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return def; }
    }
    private Integer asInt(Object o, Integer def){
        if (o == null) return def;
        try { return Integer.valueOf(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
