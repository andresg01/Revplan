package com.revplan.llm.domain.repository;

import com.revplan.llm.domain.model.Limits;

public interface LimitsRepository {
    Limits fetchLimits();
}
