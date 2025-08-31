package com.revplan.llm.infra.persistence.jpa.repository;

import com.revplan.llm.domain.model.Limits;
import com.revplan.llm.domain.repository.LimitsRepository;
import com.revplan.llm.infra.config.LimitsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Adapter de límites para OpenAI.
 * De momento usa configuración local (snapshot) y deja preparado el punto
 * para futuras lecturas de cabeceras/telemetría del proveedor.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class OpenAiLimitsAdapter implements LimitsRepository {

    private final LimitsProperties props;

    @Override
    public Limits fetchLimits() {
        Integer limit = props.getLimitPerMinute();
        Integer usage = safeNonNegative(props.getBaselineEstimatedUsage());
        Integer burst = props.getBurstAllowed();

        return Limits.builder()
                .limitPerMinute(limit)
                .estimatedUsageMinute(usage)
                .burstAllowed(burst)
                .build();
    }

    private Integer safeNonNegative(Integer v) {
        if (v == null) return 0;
        return Math.max(0, v);
    }
}
