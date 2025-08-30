package com.revplan.llm.domain.service;

import java.math.BigDecimal;

public interface FxRateService {
    /**
     * Obtiene el tipo de cambio con cache temporal (TTL).
     */
    BigDecimal getRate(String from, String to);
}
