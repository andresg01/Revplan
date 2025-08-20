package com.revapp.planengine.application.service.utils;

import com.revapp.planengine.domain.model.GeneratePlanRequest;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PromptBuilder {

    public String build(GeneratePlanRequest req, Plan base) {
        PlanKPIs k = base.getKpis();
        PlanAdjustments a = base.getAdjustments();

        // Valores seguros (evita "null")
        String incomeNet          = safe(req.getProfile()!=null ? req.getProfile().getIncomeNet() : null);
        String expensesFixed      = safe(req.getProfile()!=null ? req.getProfile().getExpensesFixed() : null);
        String expensesVariable   = safe(req.getProfile()!=null ? req.getProfile().getExpensesVariable() : null);
        String riskScore          = String.valueOf(req.getProfile()!=null ? req.getProfile().getRiskScore() : null);
        String goals              = String.valueOf(req.getProfile()!=null ? req.getProfile().getGoals() : null);

        String forcedTemplateId   = safe(req.getForceTemplateId());

        String baseSavingRate     = safe(k!=null ? k.getSavingRate() : null);
        String baseRunwayMonths   = safe(k!=null ? k.getRunwayMonths() : null);
        String baseCompliance     = safe(k!=null ? k.getBudgetCompliance() : null);

        String baseSavingPct      = safe(a!=null ? a.getSavingPct() : null);
        String baseEmergencyMonths= safe(a!=null ? a.getEmergencyMonths() : null);
        String baseEnvelopes      = formatEnvelopes(a!=null ? a.getEnvelopes() : null);

        return """
          CONTEXTO DE USUARIO (agregado, sin PII):
          - incomeNet: %s
          - expensesFixed: %s
          - expensesVariable: %s
          - riskScore: %s
          - goals: %s

          CONTEXTO DE PLANTILLA:
          - forcedTemplateId: %s   # si se indica, respétala salvo inconsistencia obvia

          BASE DETERMINISTA (reglas previas):
          - kpis.savingRate: %s
          - kpis.runwayMonths: %s
          - kpis.budgetCompliance: %s

          BASE ADJUSTMENTS (para repetir si no cambias nada):
          - adjustments.savingPct: %s
          - adjustments.emergencyMonths: %s
          - adjustments.envelopes: %s

          INSTRUCCIONES PARA EL PATCH (se refuerzan las del system):
          - Ajusta sólo dentro de los rangos del JSON Schema.
          - Si no hay cambios, repite EXACTAMENTE los valores base (ajustes y KPIs).
          - No inventes campos ni claves nuevas (additionalProperties:false).
          - Salida: únicamente JSON válido (sin texto fuera del JSON).
          """.formatted(
                incomeNet, expensesFixed, expensesVariable, riskScore, goals,
                forcedTemplateId,
                baseSavingRate, baseRunwayMonths, baseCompliance,
                baseSavingPct, baseEmergencyMonths, baseEnvelopes
        );
    }

    private static String formatEnvelopes(Map<String, BigDecimal> env) {
        if (env == null || env.isEmpty()) return "{}";
        // formateo simple clave:valor con toString() de BigDecimal
        StringBuilder sb = new StringBuilder("{ ");
        boolean first = true;
        for (var e : env.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey()).append(": ").append(e.getValue());
            first = false;
        }
        sb.append(" }");
        return sb.toString();
    }

    private static String safe(Object o){ return o==null? "null" : o.toString(); }
}
