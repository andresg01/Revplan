package com.revapp.planengine.application.service.utils;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.AiOptions;
import org.springframework.stereotype.Component;

@Component
public class PlanMerger {

    /** Rellena opciones IA faltantes con defaults. */
    public AiOptions resolveOptions(AiOptions incoming, AiDefaults defaults) {
        AiOptions o = new AiOptions();
        o.setProvider(      incoming!=null && incoming.getProvider()!=null      ? incoming.getProvider()      : defaults.getProvider());
        o.setModel(         incoming!=null && incoming.getModel()!=null         ? incoming.getModel()         : defaults.getModel());
        o.setTemperature(   incoming!=null && incoming.getTemperature()!=null   ? incoming.getTemperature()   : defaults.getTemperature());
        o.setPromptVersion( incoming!=null && incoming.getPromptVersion()!=null ? incoming.getPromptVersion() : defaults.getPromptVersion());
        return o;
    }

    /** Funde el patch IA en el plan base (rationale, alerts, adjustments, kpis). */
    public Plan merge(Plan base, Plan patch) {
        if (patch == null) return base;
        if (patch.getRationale()!=null) base.setRationale(patch.getRationale());
        if (patch.getAlerts()!=null && !patch.getAlerts().isEmpty()) base.setAlerts(patch.getAlerts());
        if (patch.getAdjustments()!=null) base.setAdjustments(patch.getAdjustments());
        if (patch.getKpis()!=null) base.setKpis(patch.getKpis());
        return base;
    }
}
