package com.revplan.llm.domain.repository;

import com.revplan.llm.domain.model.HealthLLM;

public interface HealthRepository {
    HealthLLM check();
}
