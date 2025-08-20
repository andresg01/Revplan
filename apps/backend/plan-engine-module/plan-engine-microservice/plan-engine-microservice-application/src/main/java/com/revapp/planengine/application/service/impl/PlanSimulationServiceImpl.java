package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.application.service.calc.KpiCalculator;
import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import com.revapp.planengine.domain.model.PlanSimulationData;
import com.revapp.planengine.domain.model.PlanSimulationScenario;
import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.domain.repository.PlanVersionRepository;
import com.revapp.planengine.domain.service.PlanSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanSimulationServiceImpl implements PlanSimulationService {

    private final PlanVersionRepository planVersionRepository;

    @Override
    public PlanSimulationData simulate(UUID planId, PlanAdjustments adjustments) {
        log.debug("PlanSimulation.simulate(planId={}, adjustments={})", planId, adjustments);
        if (planId == null) {
            log.warn("PlanSimulation.simulate -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }

        Instant t0 = Instant.now();
        // 1) KPIs base (última versión del plan)
        PlanKPIs base = planVersionRepository.findLatest(planId)
                .map(PlanVersion::getKpis)
                .orElseGet(() -> new PlanKPIs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // 2) Escenario a partir de ajustes
        BigDecimal baseSaving = Optional.ofNullable(base.getSavingRate()).orElse(BigDecimal.ZERO);
        BigDecimal newSaving  = (adjustments != null && adjustments.getSavingPct() != null)
                ? adjustments.getSavingPct()
                : baseSaving;

        PlanKPIs scenarioKPIs = new PlanKPIs(
                KpiCalculator.savingRate(newSaving),
                KpiCalculator.runwayMonthsScaled(base.getRunwayMonths(), baseSaving, newSaving),
                KpiCalculator.budgetCompliance(adjustments != null ? adjustments.getEnvelopes() : null)
        );

        PlanSimulationScenario scenario = new PlanSimulationScenario(adjustments, scenarioKPIs);
        PlanSimulationData data = new PlanSimulationData(base, scenario);

        log.info("PlanSimulation.simulate(planId={}) -> done in {}", planId, Duration.between(t0, Instant.now()));
        log.debug("PlanSimulation.simulate(planId={}) -> baseKPIs={}, scenarioKPIs={}", planId, base, scenarioKPIs);
        return data;
    }
}
