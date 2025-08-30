package com.revplan.llm.domain.repository;

import java.math.BigDecimal;

public interface FxRateRepository {
    /**
     * Devuelve el tipo de cambio directo FROM->TO (por ejemplo USD->EUR).
     * Si FROM == TO, devuelve 1.
     */
    BigDecimal getRate(String from, String to);
}
