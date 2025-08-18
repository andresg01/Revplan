/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.application.service.calc.KpiCalculator;
import com.revapp.planengine.domain.enums.PlanSourceEnum;
import com.revapp.planengine.domain.enums.PlanStatusEnum;
import com.revapp.planengine.domain.enums.SpendCategoryEnum;
import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.AccountsAggregate;
import com.revapp.planengine.domain.model.GeneratePlanRequest;
import com.revapp.planengine.domain.model.Money;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import com.revapp.planengine.domain.model.PlanVersion;
import com.revapp.planengine.domain.model.ProfileSnapshot;
import com.revapp.planengine.domain.model.SpendCategoryAmount;
import com.revapp.planengine.domain.model.SpendSummary;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.repository.PlanVersionRepository;
import com.revapp.planengine.domain.service.PlanGenerationService;
import com.revapp.planengine.domain.utils.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Caso de uso: generar/regenerar plan.
 * Validaciones de negocio en servicio (hexagonal) + defensa ante condiciones de carrera.
 */
@Service
@RequiredArgsConstructor
public class PlanGenerationServiceImpl implements PlanGenerationService {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TOL = new BigDecimal("0.02"); // tolerancia para envelopes

    private final PlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanReadRepository planReadRepository;
    private final PlanTemplateRepository planTemplateRepository;

    @Override
    @Transactional
    public Plan generate(GeneratePlanRequest request) {
        // -------- 0) Precondiciones de entrada --------
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Request body is required");
        }
        UUID userId = request.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        String templateId = Optional.ofNullable(request.getForceTemplateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSING_PARAMETER, "forceTemplateId is required"));

        // 0.1) Existencia de plantilla (404)
        planTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + templateId));

        // 1) Recuperar/crear plan del usuario (único por user_id)
        Plan plan = findOrCreateUniquePlan(userId);

        // 2) Ajustes iniciales (con reglas deterministas)
        PlanAdjustments adj = buildInitialAdjustments(request);

        // 2.1) Validaciones de negocio sobre ajustes
        validateAdjustments(adj);

        // 3) KPIs iniciales (deterministas)
        PlanKPIs kpis = buildInitialKPIs(request, adj);
        validateKpis(kpis); // formato/valores válidos

        // 4) Siguiente versión (>=1)
        int nextVersion = planVersionRepository.findLatest(plan.getId())
                .map(PlanVersion::getVersion)
                .map(v -> v + 1)
                .orElse(1);

        // 5) Construir versión
        PlanVersion version = new PlanVersion();
        version.setVersion(nextVersion);
        version.setTemplateId(templateId);
        version.setAdjustments(adj);
        version.setKpis(kpis);
        version.setRationale(Boolean.TRUE.equals(request.isExplain())
                ? "Generado por " + (request.getMode() == PlanSourceEnum.RULES ? "rules" : "rules_plus_gpt")
                : null);
        version.setAlerts(new ArrayList<>());
        version.setSource(request.getMode() == null ? PlanSourceEnum.RULES_PLUS_GPT : request.getMode());
        version.setCreatedAt(LocalDateTime.now());

        // 6) Persistir versión (la DB valida de nuevo con trigger/constraints)
        planVersionRepository.save(plan.getId(), version);

        // 7) Activar versión
        planRepository.updateActiveVersion(plan.getId(), nextVersion);

        // 8) Devolver plan activo hidratado
        return planReadRepository.findActiveById(plan.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ILLEGAL_STATE, "Active plan not found after generation"));
    }

    // ---------- creación/obtención de plan único por usuario ----------
    private Plan findOrCreateUniquePlan(UUID userId) {
        // Intenta recuperar el plan existente
        var page = planRepository.findByUser(userId, false, PageRequest.of(0, 1));
        var existing = page.items().stream().findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        // Si no existe, créalo (resiliente a condiciones de carrera mediante relectura)
        try {
            Plan p = new Plan();
            p.setUserId(userId);
            p.setStatus(com.revapp.planengine.domain.enums.PlanStatusEnum.ACTIVE);
            return planRepository.save(p);
        } catch (DataIntegrityViolationException e) {
            // Posible carrera por UNIQUE(user_id): re-leemos
            var retry = planRepository.findByUser(userId, false, PageRequest.of(0, 1))
                    .items().stream().findFirst();
            if (retry.isPresent()) return retry.get();
            throw new BusinessException(ErrorCode.INTEGRITY_VIOLATION, "Could not create unique plan for user " + userId);
        }
    }

    // ---------- reglas deterministas sin mocks ----------

    private PlanAdjustments buildInitialAdjustments(GeneratePlanRequest req) {
        BigDecimal savingPct = inferSavingPct(req.getProfile());
        Integer emergencyMonths = inferEmergencyMonths(req.getProfile());
        Map<String, BigDecimal> envelopes = inferEnvelopes(req.getSpend());
        return new PlanAdjustments(savingPct, emergencyMonths, envelopes);
    }

    private PlanKPIs buildInitialKPIs(GeneratePlanRequest req, PlanAdjustments adj) {
        BigDecimal savingRate = adj.getSavingPct();
        BigDecimal runway = inferRunwayMonths(req);
        BigDecimal compliance = KpiCalculator.budgetCompliance(adj.getEnvelopes());
        return new PlanKPIs(savingRate, runway, compliance);
    }

    private BigDecimal inferSavingPct(ProfileSnapshot profile) {
        if (profile == null) return bd(0.20);
        BigDecimal inc = nz(profile.getIncomeNet());
        BigDecimal out = nz(profile.getExpensesFixed()).add(nz(profile.getExpensesVariable()));
        if (inc.compareTo(bd(1e-6)) <= 0) return ZERO;
        BigDecimal s = inc.subtract(out).divide(inc, 8, RoundingMode.HALF_UP);
        if (s.compareTo(ZERO) < 0) s = ZERO;
        if (s.compareTo(ONE) > 0) s = ONE;
        return s;
    }

    private Integer inferEmergencyMonths(ProfileSnapshot profile) {
        if (profile == null) return 3;
        int d = Optional.ofNullable(profile.getDependents()).orElse(0);
        int base = 3;
        if (d >= 1) base += 2;
        if (d >= 3) base += 1;
        return Math.min(24, Math.max(0, base));
    }

    private Map<String, BigDecimal> inferEnvelopes(SpendSummary spend) {
        Map<String, BigDecimal> env = new LinkedHashMap<>();
        if (spend == null || spend.getCategories() == null || spend.getCategories().isEmpty()) {
            env.put("fixed", bd(0.50));
            env.put("variable", bd(0.30));
            env.put("goals", bd(0.20));
            return env;
        }

        BigDecimal total = ZERO;
        for (SpendCategoryAmount c : spend.getCategories()) {
            if (c == null || c.getAmount() == null) continue;
            total = total.add(c.getAmount());
        }
        if (total.compareTo(bd(1e-6)) <= 0) {
            env.put("fixed", bd(0.50));
            env.put("variable", bd(0.30));
            env.put("goals", bd(0.20));
            return env;
        }

        BigDecimal fixed = ZERO;
        BigDecimal variable = ZERO;

        for (SpendCategoryAmount c : spend.getCategories()) {
            if (c == null || c.getCategory() == null || c.getAmount() == null) continue;
            SpendCategoryEnum cat = c.getCategory();
            if (cat == SpendCategoryEnum.RENT || cat == SpendCategoryEnum.UTILITIES || cat == SpendCategoryEnum.EDUCATION) {
                fixed = fixed.add(c.getAmount());
            } else {
                variable = variable.add(c.getAmount());
            }
        }

        BigDecimal fixedPct = fixed.divide(total, 8, RoundingMode.HALF_UP);
        BigDecimal varPct   = variable.divide(total, 8, RoundingMode.HALF_UP);
        BigDecimal goalsPct = ONE.subtract(fixedPct.add(varPct), MathContext.DECIMAL64);
        if (goalsPct.compareTo(ZERO) < 0) goalsPct = ZERO;

        env.put("fixed", clamp01(fixedPct));
        env.put("variable", clamp01(varPct));
        env.put("goals", clamp01(goalsPct));
        return env;
    }

    private BigDecimal inferRunwayMonths(GeneratePlanRequest req) {
        BigDecimal cash = Optional.ofNullable(req.getAccounts())
                .map(AccountsAggregate::getTotalBalance)
                .map(Money::getAmount)
                .orElse(ZERO);

        BigDecimal monthlyOut = nz(req.getProfile() != null ? req.getProfile().getExpensesFixed() : null)
                .add(nz(req.getProfile() != null ? req.getProfile().getExpensesVariable() : null));

        if (monthlyOut.compareTo(bd(1e-6)) <= 0) return ZERO;
        return cash.divide(monthlyOut, 8, RoundingMode.HALF_UP);
    }

    // ---------- validaciones de negocio previas a persistir ----------

    private void validateAdjustments(PlanAdjustments adj) {
        if (adj == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments is required");
        }
        // saving_pct 0..1
        if (adj.getSavingPct() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments.saving_pct is required");
        }
        if (adj.getSavingPct().compareTo(ZERO) < 0 || adj.getSavingPct().compareTo(ONE) > 0) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "adjustments.saving_pct must be between 0 and 1");
        }
        // emergency_months 0..24
        if (adj.getEmergencyMonths() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments.emergency_months is required");
        }
        if (adj.getEmergencyMonths() < 0 || adj.getEmergencyMonths() > 24) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "adjustments.emergency_months must be between 0 and 24");
        }
        // envelopes suma ≈ 1 si existe
        Map<String, BigDecimal> env = adj.getEnvelopes();
        if (env != null && !env.isEmpty()) {
            BigDecimal sum = ZERO;
            for (Map.Entry<String, BigDecimal> e : env.entrySet()) {
                BigDecimal v = e.getValue();
                if (v == null || v.compareTo(ZERO) < 0 || v.compareTo(ONE) > 0) {
                    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
                            "adjustments.envelopes[" + e.getKey() + "] must be between 0 and 1");
                }
                sum = sum.add(v);
            }
            if (!withinTolerance(sum, ONE, TOL)) {
                throw new BusinessException(ErrorCode.ENVELOPE_SUM_INVALID,
                        "adjustments.envelopes must sum ~1 (±" + TOL + "), got " + sum);
            }
        }
    }

    private void validateKpis(PlanKPIs kpis) {
        if (kpis == null) {
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis is required");
        }
        if (kpis.getSavingRate() == null || kpis.getSavingRate().compareTo(ZERO) < 0 || kpis.getSavingRate().compareTo(ONE) > 0) {
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis.savingRate must be between 0 and 1");
        }
        if (kpis.getRunwayMonths() == null || kpis.getRunwayMonths().compareTo(ZERO) < 0) {
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis.runwayMonths must be >= 0");
        }
        if (kpis.getBudgetCompliance() == null || kpis.getBudgetCompliance().compareTo(ZERO) < 0 || kpis.getBudgetCompliance().compareTo(ONE) > 0) {
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis.budgetCompliance must be between 0 and 1");
        }
    }

    // -------- helpers numéricos --------
    private static BigDecimal bd(double d){ return new BigDecimal(String.valueOf(d)); }
    private static BigDecimal nz(BigDecimal x){ return x == null ? ZERO : x; }
    private static BigDecimal clamp01(BigDecimal x){
        if (x.compareTo(ZERO) < 0) return ZERO;
        if (x.compareTo(ONE) > 0) return ONE;
        return x;
    }
    private static boolean withinTolerance(BigDecimal value, BigDecimal target, BigDecimal tol) {
        BigDecimal min = target.subtract(tol);
        BigDecimal max = target.add(tol);
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
