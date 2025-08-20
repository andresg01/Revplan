package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.enums.CurrencyEnum;
import com.revapp.planengine.domain.enums.GoalEnum;
import com.revapp.planengine.domain.enums.HorizonEnum;
import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.infra.api.dto.*;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.*;

@Mapper(componentModel = "spring")
public interface RequestsApiMapper {

    // ---- UpdatePlanRequest (solo adjustments) ----
    default PlanAdjustments toModel(UpdatePlanRequestDTO dto) {
        if (dto == null) return null;
        PlanAdjustmentsDTO a = dto.getAdjustments();
        if (a == null) return null;
        PlanAdjustments m = new PlanAdjustments();
        m.setSavingPct(a.getSavingPct());
        m.setEmergencyMonths(a.getEmergencyMonths());
        m.setEnvelopes(a.getEnvelopes());
        return m;
    }

    // ---- Recompute reason ----
    default RecomputeReasonEnum toModel(RecomputePlanRequestDTO.ReasonEnum r) {
        if (r == null) return RecomputeReasonEnum.MANUAL;
        return switch (r) {
            case BALANCE_CHANGE -> RecomputeReasonEnum.BALANCE_CHANGE;
            case SPEND_DRIFT -> RecomputeReasonEnum.SPEND_DRIFT;
            case PERIODIC_REVIEW -> RecomputeReasonEnum.PERIODIC_REVIEW;
            case MANUAL -> RecomputeReasonEnum.MANUAL;
        };
    }

    // ---- Simulate: domain -> DTO ----
    default PlanSimulationDataDTO toDto(PlanSimulationData sim) {
        if (sim == null) return null;
        PlanSimulationDataDataDTO data = new PlanSimulationDataDataDTO();
        data.setBase(toDto(sim.getBase()));

        PlanSimulationDataDataScenarioDTO sc = new PlanSimulationDataDataScenarioDTO();
        sc.setChanges(toDto(sim.getScenario() != null ? sim.getScenario().getChanges() : null));
        sc.setKpis(toDto(sim.getScenario() != null ? sim.getScenario().getKpis() : null));
        data.setScenario(sc);

        PlanSimulationDataDTO dto = new PlanSimulationDataDTO();
        dto.setData(data);
        return dto;
    }

    default PlanKPIsDTO toDto(PlanKPIs k) {
        if (k == null) return null;
        PlanKPIsDTO dto = new PlanKPIsDTO();
        dto.setSavingRate(k.getSavingRate());
        dto.setRunwayMonths(k.getRunwayMonths());
        dto.setBudgetCompliance(k.getBudgetCompliance());
        return dto;
    }

    default PlanAdjustmentsDTO toDto(PlanAdjustments a) {
        if (a == null) return null;
        PlanAdjustmentsDTO dto = new PlanAdjustmentsDTO();
        dto.setSavingPct(a.getSavingPct());
        dto.setEmergencyMonths(a.getEmergencyMonths());
        dto.setEnvelopes(a.getEnvelopes());
        return dto;
    }

    // ---- GeneratePlanRequest ----
    default GeneratePlanRequest toModel(GeneratePlanRequestDTO dto) {
        if (dto == null) return null;
        GeneratePlanRequest req = GeneratePlanRequest.builder()
                .userId(dto.getUserId())
                .mode(dto.getMode() == GeneratePlanRequestDTO.ModeEnum.RULES
                        ? com.revapp.planengine.domain.enums.PlanSourceEnum.RULES
                        : com.revapp.planengine.domain.enums.PlanSourceEnum.RULES_PLUS_GPT)
                .forceTemplateId(dto.getForceTemplateId())
                .profile(toModel(dto.getProfile()))
                .accounts(toModel(dto.getAccounts()))
                .spend(toModel(dto.getSpend()))
                .debts(toModel(dto.getDebts()))
                .explain(Boolean.TRUE.equals(dto.getExplain()))
                .aiOptions(toModel(dto.getAiOptions()))
                .build();
        return req;
    }

    // ---- Profile ----
    default ProfileSnapshot toModel(ProfileSnapshotDTO dto) {
        if (dto == null) return null;
        ProfileSnapshot.ProfileSnapshotBuilder b = ProfileSnapshot.builder();
        b.age(dto.getAge());
        b.stabilityIndex(dto.getStabilityIndex());
        b.liquidityNeed(dto.getLiquidityNeed());
        b.riskScore(dto.getRiskScore());
        b.debtSeverity(dto.getDebtSeverity());
        b.incomeNet(toBig(dto.getIncomeNet()));
        b.expensesFixed(toBig(dto.getExpensesFixed()));
        b.expensesVariable(toBig(dto.getExpensesVariable()));
        b.dependents(dto.getDependents());
        b.horizon(toModel(dto.getHorizon()));

        List<GoalEnum> goals = new ArrayList<>();
        if (dto.getGoals() != null) {
            for (GoalDTO g : dto.getGoals()) goals.add(toModel(g));
        }
        b.goals(goals);
        return b.build();
    }

    default HorizonEnum toModel(ProfileSnapshotDTO.HorizonEnum h) {
        if (h == null) return null;
        return switch (h) { case SHORT -> HorizonEnum.SHORT; case MEDIUM -> HorizonEnum.MEDIUM; case LONG -> HorizonEnum.LONG; };
    }

    default GoalEnum toModel(GoalDTO g) {
        if (g == null) return null;
        return switch (g) {
            case EMERGENCY -> GoalEnum.EMERGENCY;
            case HOUSING -> GoalEnum.HOUSING;
            case TRAVEL -> GoalEnum.TRAVEL;
            case INVESTMENT -> GoalEnum.INVESTMENT;
            case DEBT_REDUCTION -> GoalEnum.DEBT_REDUCTION;
        };
    }

    // ---- AccountsAggregate ----
    default AccountsAggregate toModel(AccountsAggregateDTO dto) {
        if (dto == null) return null;
        AccountsAggregate.AccountsAggregateBuilder b = AccountsAggregate.builder();
        b.totalBalance(toModel(dto.getTotalBalance()));

        Map<CurrencyEnum, BigDecimal> map = new EnumMap<>(CurrencyEnum.class);
        if (dto.getByCurrency() != null) {
            dto.getByCurrency().forEach(row -> {
                if (row.getCurrency() != null && row.getBalance() != null) {
                    map.put(toModel(row.getCurrency()), toBig(row.getBalance()));
                }
            });
        }
        b.byCurrency(map);
        return b.build();
    }

    default Money toModel(MoneyDTO dto) {
        if (dto == null) return null;
        return Money.builder()
                .amount(toBig(dto.getAmount()))
                .currency(toModel(dto.getCurrency()))
                .build();
    }

    default CurrencyEnum toModel(CurrencyDTO c) {
        if (c == null) return null;
        return CurrencyEnum.valueOf(c.name()); // EUR, USD, GBP, CHF, JPY
    }

    // ---- SpendSummary ----
    default SpendSummary toModel(SpendSummaryDTO dto) {
        if (dto == null) return null;
        SpendSummary.SpendSummaryBuilder b = SpendSummary.builder();
        b.periodDays(dto.getPeriodDays());
        b.totalOut(toBig(dto.getTotalOut()));
        List<SpendCategoryAmount> items = new ArrayList<>();
        if (dto.getCategories() != null) {
            for (SpendSummaryCategoriesInnerDTO in : dto.getCategories()) {
                SpendCategoryAmount a = SpendCategoryAmount.builder()
                        .category(toModel(in.getCategory()))
                        .amount(toBig(in.getAmount()))
                        .build();
                items.add(a);
            }
        }
        b.categories(items);
        return b.build();
    }

    default com.revapp.planengine.domain.enums.SpendCategoryEnum toModel(SpendCategoryDTO c) {
        if (c == null) return null;
        return com.revapp.planengine.domain.enums.SpendCategoryEnum.valueOf(c.name());
    }

    // ---- Debts ----
    default DebtSummary toModel(DebtSummaryDTO dto) {
        if (dto == null) return null;
        DebtSummary.DebtSummaryBuilder b = DebtSummary.builder();
        List<DebtItem> items = new ArrayList<>();
        if (dto.getItems() != null) {
            for (DebtItemDTO d : dto.getItems()) {
                items.add(DebtItem.builder()
                        .id(d.getId())
                        .apr(d.getApr())
                        .remainingPrincipal(toBig(d.getRemainingPrincipal()))
                        .minPayment(toBig(d.getMinPayment()))
                        .build());
            }
        }
        b.items(items);
        b.totalRemaining(toBig(dto.getTotalRemaining()));
        return b.build();
    }

    // AiOptions (DTO -> domain) ----
    default com.revapp.planengine.domain.model.AiOptions toModel(AiOptionsDTO dto) {
        if (dto == null) return null;
        Double temperature = dto.getTemperature() != null ? dto.getTemperature().doubleValue() : null;
        return com.revapp.planengine.domain.model.AiOptions.builder()
                .provider(dto.getProvider())
                .model(dto.getModel())
                .temperature(temperature)
                .promptVersion(dto.getPromptVersion())
                .build();
    }

    // ---- helpers ----
    private static BigDecimal toBig(Double d) { return d == null ? null : BigDecimal.valueOf(d); }
}
