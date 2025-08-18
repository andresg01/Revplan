package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.application.service.calc.KpiCalculator;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import com.revapp.planengine.domain.model.PlanSimulationData;
import com.revapp.planengine.domain.model.PlanSimulationScenario;
import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.domain.repository.PlanVersionRepository;
import com.revapp.planengine.domain.service.PlanSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanSimulationServiceImpl implements PlanSimulationService {

    private final PlanVersionRepository planVersionRepository;

    @Override
    public PlanSimulationData simulate(UUID planId, PlanAdjustments adjustments) {
        // 1) KPIs base reales: última versión del plan
        PlanKPIs base = planVersionRepository.findLatest(planId)
                .map(PlanVersion::getKpis)
                .orElseGet(() -> new PlanKPIs(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                ));

        // 2) Escenario a partir de ajustes
        BigDecimal baseSaving = Optional.ofNullable(base.getSavingRate()).orElse(BigDecimal.ZERO);
        BigDecimal newSaving  = adjustments != null && adjustments.getSavingPct() != null
                ? adjustments.getSavingPct()
                : baseSaving;

        PlanKPIs scenarioKPIs = new PlanKPIs(
                KpiCalculator.savingRate(newSaving),
                KpiCalculator.runwayMonthsScaled(base.getRunwayMonths(), baseSaving, newSaving),
                KpiCalculator.budgetCompliance(adjustments != null ? adjustments.getEnvelopes() : null)
        );

        PlanSimulationScenario scenario = new PlanSimulationScenario(adjustments, scenarioKPIs);
        return new PlanSimulationData(base, scenario);
    }
}
