package com.revapp.planengine.domain.repository;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.AiOptions;
import com.revapp.planengine.domain.model.AiResult;

/** Puerto de dominio para generación/ajuste de Plan vía LLM. */
public interface PlanAiRepository {
    /** Genera/ajusta un plan a partir de un “plan base” y contexto (prompt) usando un LLM. */
    AiResult generateFromBase(Plan base, String promptUser, AiOptions options);
}
