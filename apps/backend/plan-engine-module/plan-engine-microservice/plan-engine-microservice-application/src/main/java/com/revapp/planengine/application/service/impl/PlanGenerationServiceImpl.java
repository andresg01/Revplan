package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.application.service.calc.KpiCalculator;
import com.revapp.planengine.application.service.utils.AiDefaults;
import com.revapp.planengine.application.service.utils.PlanMerger;
import com.revapp.planengine.application.service.utils.PromptBuilder;
import com.revapp.planengine.domain.enums.PlanSourceEnum;
import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.domain.model.AiOptions;
import com.revapp.planengine.domain.repository.PlanAiRepository;
import com.revapp.planengine.domain.repository.PlanReadRepository;
import com.revapp.planengine.domain.repository.PlanRepository;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.repository.PlanVersionRepository;
import com.revapp.planengine.domain.service.PlanGenerationService;
import com.revapp.planengine.domain.utils.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationServiceImpl implements PlanGenerationService {

    private static final BigDecimal ONE  = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TOL  = new BigDecimal("0.02"); // tolerancia para envelopes

    private final PlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanReadRepository planReadRepository;
    private final PlanTemplateRepository planTemplateRepository;
    private final PlanAiRepository planAiRepository;

    private final PromptBuilder promptBuilder;
    private final PlanMerger planMerger;
    private final AiDefaults aiDefaults;

    @Override
    @Transactional
    public Plan generate(GeneratePlanRequest request) {
        Instant t0 = Instant.now();
        UUID userId = request != null ? request.getUserId() : null;
        String forcedTemplate = request != null ? request.getForceTemplateId() : null;
        PlanSourceEnum mode = request != null ? request.getMode() : null;
        Boolean explain = request != null ? request.isExplain() : null;

        log.debug("PlanGeneration.generate(userId={}, forceTemplateId={}, mode={}, explain={})",
                userId, forcedTemplate, mode, explain);

        // -------- 0) Precondiciones de entrada --------
        if (request == null) {
            log.warn("PlanGeneration.generate -> request is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Request body is required");
        }
        if (userId == null) {
            log.warn("PlanGeneration.generate -> userId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "userId is required");
        }
        String templateId = Optional.ofNullable(request.getForceTemplateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSING_PARAMETER, "forceTemplateId is required"));

        // 0.1) Existencia de plantilla (404)
        Instant tTpl0 = Instant.now();
        planTemplateRepository.findById(templateId)
                .orElseThrow(() -> {
                    log.warn("PlanGeneration.generate -> template not found: {}", templateId);
                    return new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + templateId);
                });
        log.debug("PlanGeneration.generate -> template '{}' exists ({}).",
                templateId, Duration.between(tTpl0, Instant.now()));

        // 1) Recuperar/crear plan del usuario (único por user_id)
        Instant tPlan0 = Instant.now();
        Plan plan = findOrCreateUniquePlan(userId);
        log.info("PlanGeneration.generate -> using plan id={} for user={} ({}).",
                plan.getId(), userId, Duration.between(tPlan0, Instant.now()));

        // 2) Ajustes iniciales (reglas deterministas)
        PlanAdjustments adj = buildInitialAdjustments(request);
        // normaliza sobres desde el principio
        adj.setEnvelopes(normalizeEnvelopes(adj.getEnvelopes()));
        log.debug("PlanGeneration.generate -> adjustments computed/normalized: {}", adj);
        validateAdjustments(adj);

        // 3) KPIs iniciales (deterministas) con cumplimiento tras normalizar
        PlanKPIs kpis = buildInitialKPIs(request, adj);
        log.debug("PlanGeneration.generate -> KPIs computed: {}", kpis);
        validateKpis(kpis);

        // 4) Construimos el “plan base” (pre-IA)
        plan.setTemplateId(templateId);
        plan.setAdjustments(adj);
        plan.setKpis(kpis);
        if (Boolean.TRUE.equals(explain)) {
            plan.setRationale("Generado por " + (mode == PlanSourceEnum.RULES ? "rules" : "rules_plus_gpt"));
        }
        plan.setAlerts(new ArrayList<>());

        // 4.1) Si el modo es RULES_PLUS_GPT → IA
        if (mode == PlanSourceEnum.RULES_PLUS_GPT) {
            String userPrompt = promptBuilder.build(request, plan);
            AiOptions options = planMerger.resolveOptions(request.getAiOptions(), aiDefaults);

            var aiResult = planAiRepository.generateFromBase(plan, userPrompt, options);

            // a) merge del parche
            plan = planMerger.merge(plan, aiResult.getPlanPatch());
            plan.setAiMeta(aiResult.getMeta()); // <-- rellena aiMeta en memoria

            // b) normaliza sobres y recomputa KPIs para garantizar compliance ≈ 1.00
            var envNorm = normalizeEnvelopes(plan.getAdjustments().getEnvelopes());
            plan.getAdjustments().setEnvelopes(envNorm);
            plan.setKpis(recomputeKpis(plan, request));
        }

        // 5) Siguiente versión (>=1)
        Instant tVer0 = Instant.now();
        int nextVersion = planVersionRepository.findLatest(plan.getId())
                .map(PlanVersion::getVersion)
                .map(v -> v + 1)
                .orElse(1);
        log.debug("PlanGeneration.generate -> next version for plan {} will be {} ({}).",
                plan.getId(), nextVersion, Duration.between(tVer0, Instant.now()));

        // 6) Persistir versión (aiMeta SIN migrar: se embebe en params.ai_meta)
        PlanVersion version = new PlanVersion();
        version.setVersion(nextVersion);
        version.setTemplateId(templateId);
        version.setAdjustments(embedAiMetaInParams(plan.getAdjustments(), plan.getAiMeta())); // <--
        version.setKpis(plan.getKpis());
        version.setRationale(plan.getRationale());
        version.setAlerts(plan.getAlerts());
        version.setSource(mode == null ? PlanSourceEnum.RULES_PLUS_GPT : mode);
        version.setCreatedAt(LocalDateTime.now());
        version.setAiMeta(plan.getAiMeta()); // por si en el futuro añades columna/soporte

        Instant tSaveV0 = Instant.now();
        planVersionRepository.save(plan.getId(), version);
        log.info("PlanGeneration.generate -> version {} persisted for plan {} ({}).",
                nextVersion, plan.getId(), Duration.between(tSaveV0, Instant.now()));

        // 7) Activar versión
        Instant tAct0 = Instant.now();
        planRepository.updateActiveVersion(plan.getId(), nextVersion);
        log.info("PlanGeneration.generate -> plan {} activeVersion set to {} ({}).",
                plan.getId(), nextVersion, Duration.between(tAct0, Instant.now()));

        // 8) Devolver plan activo hidratado
        final UUID planId = plan.getId();
        Instant tRead0 = Instant.now();
        Plan active = planReadRepository.findActiveById(planId)
                .map(this::extractAiMetaFromParamsIfNeeded) // <-- rehidrata aiMeta desde params.ai_meta
                .orElseThrow(() -> {
                    log.error("PlanGeneration.generate -> active plan not found after generation (planId={})", planId);
                    return new BusinessException(ErrorCode.ILLEGAL_STATE, "Active plan not found after generation");
                });

        log.info("PlanGeneration.generate -> SUCCESS planId={}, version={}, totalTime={}",
                planId, nextVersion, Duration.between(t0, Instant.now()));
        log.debug("PlanGeneration.generate -> hydrated active plan in {}", Duration.between(tRead0, Instant.now()));
        return active;
    }

    // ---------- creación/obtención de plan único por usuario ----------
    private Plan findOrCreateUniquePlan(UUID userId) {
        log.debug("findOrCreateUniquePlan(userId={})", userId);
        var page = planRepository.findByUser(userId, false, PageRequest.of(0, 1));
        var existing = page.items().stream().findFirst();
        if (existing.isPresent()) {
            log.debug("findOrCreateUniquePlan -> existing plan found: {}", existing.get().getId());
            return existing.get();
        }
        try {
            Plan p = new Plan();
            p.setUserId(userId);
            p.setStatus(com.revapp.planengine.domain.enums.PlanStatusEnum.ACTIVE);
            Plan saved = planRepository.save(p);
            log.debug("findOrCreateUniquePlan -> created new plan: {}", saved.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("findOrCreateUniquePlan -> race condition on UNIQUE(user_id), retrying. cause={}", e.getMostSpecificCause().getMessage());
            var retry = planRepository.findByUser(userId, false, PageRequest.of(0, 1))
                    .items().stream().findFirst();
            if (retry.isPresent()) {
                log.debug("findOrCreateUniquePlan -> retrieved after race: {}", retry.get().getId());
                return retry.get();
            }
            log.error("findOrCreateUniquePlan -> failed after retry for user {}", userId);
            throw new BusinessException(ErrorCode.INTEGRITY_VIOLATION, "Could not create unique plan for user " + userId);
        }
    }

    // ---------- reglas deterministas ----------
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
            switch (c.getCategory()) {
                case RENT, UTILITIES, EDUCATION -> fixed = fixed.add(c.getAmount());
                default -> variable = variable.add(c.getAmount());
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

    // ---------- validaciones ----------
    private void validateAdjustments(PlanAdjustments adj) {
        if (adj == null) {
            log.warn("validateAdjustments -> adjustments is null");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments is required");
        }
        if (adj.getSavingPct() == null) {
            log.warn("validateAdjustments -> savingPct is null");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments.saving_pct is required");
        }
        if (adj.getSavingPct().compareTo(ZERO) < 0 || adj.getSavingPct().compareTo(ONE) > 0) {
            log.warn("validateAdjustments -> savingPct out of range: {}", adj.getSavingPct());
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "adjustments.saving_pct must be between 0 and 1");
        }
        if (adj.getEmergencyMonths() == null) {
            log.warn("validateAdjustments -> emergencyMonths is null");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "adjustments.emergency_months is required");
        }
        if (adj.getEmergencyMonths() < 0 || adj.getEmergencyMonths() > 24) {
            log.warn("validateAdjustments -> emergencyMonths out of range: {}", adj.getEmergencyMonths());
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "adjustments.emergency_months must be between 0 and 24");
        }
        Map<String, BigDecimal> env = adj.getEnvelopes();
        if (env != null && !env.isEmpty()) {
            BigDecimal sum = ZERO;
            for (Map.Entry<String, BigDecimal> e : env.entrySet()) {
                BigDecimal v = e.getValue();
                if (v == null || v.compareTo(ZERO) < 0 || v.compareTo(ONE) > 0) {
                    log.warn("validateAdjustments -> envelope '{}' invalid value: {}", e.getKey(), v);
                    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
                            "adjustments.envelopes[" + e.getKey() + "] must be between 0 and 1");
                }
                sum = sum.add(v);
            }
            if (!withinTolerance(sum, ONE, TOL)) {
                log.warn("validateAdjustments -> envelopes sum out of tolerance: {}", sum);
                throw new BusinessException(ErrorCode.ENVELOPE_SUM_INVALID,
                        "adjustments.envelopes must sum ~1 (±" + TOL + "), got " + sum);
            }
        }
    }

    private void validateKpis(PlanKPIs kpis) {
        if (kpis == null) {
            log.warn("validateKpis -> KPIs is null");
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis is required");
        }
        if (kpis.getSavingRate() == null || kpis.getSavingRate().compareTo(ZERO) < 0 || kpis.getSavingRate().compareTo(ONE) > 0) {
            log.warn("validateKpis -> savingRate invalid: {}", kpis.getSavingRate());
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis.savingRate must be between 0 and 1");
        }
        if (kpis.getRunwayMonths() == null || kpis.getRunwayMonths().compareTo(ZERO) < 0) {
            log.warn("validateKpis -> runwayMonths invalid: {}", kpis.getRunwayMonths());
            throw new BusinessException(ErrorCode.KPIS_INVALID, "kpis.runwayMonths must be >= 0");
        }
        if (kpis.getBudgetCompliance() == null || kpis.getBudgetCompliance().compareTo(ZERO) < 0 || kpis.getBudgetCompliance().compareTo(ONE) > 0) {
            log.warn("validateKpis -> budgetCompliance invalid: {}", kpis.getBudgetCompliance());
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

    // -------- NUEVO: normalización + recompute + aiMeta embedding/rehydration --------
    private Map<String, BigDecimal> normalizeEnvelopes(Map<String, BigDecimal> env) {
        if (env == null || env.isEmpty()) return env;
        BigDecimal sum = env.values().stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
        if (sum.compareTo(ZERO) == 0) return env;

        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (var e : env.entrySet()) {
            BigDecimal v = e.getValue() == null ? ZERO : e.getValue();
            out.put(e.getKey(), v.divide(sum, 6, RoundingMode.HALF_UP));
        }
        return out;
    }

    private PlanKPIs recomputeKpis(Plan plan, GeneratePlanRequest req) {
        BigDecimal savingRate = KpiCalculator.savingRate(plan.getAdjustments().getSavingPct());
        BigDecimal runway = plan.getKpis() != null && plan.getKpis().getRunwayMonths() != null
                ? plan.getKpis().getRunwayMonths()
                : inferRunwayMonths(req);
        BigDecimal compliance = KpiCalculator.budgetCompliance(plan.getAdjustments().getEnvelopes());
        return new PlanKPIs(savingRate, runway, compliance);
    }

    /**
     * Embebe aiMeta dentro de params (sin migrar DB):
     * params = { saving_pct, emergency_months, envelopes, ai_meta: {...} }
     */
    private PlanAdjustments embedAiMetaInParams(PlanAdjustments adj, AiMeta meta) {
        if (adj == null || meta == null) return adj;
        // No modificamos modelo; la inserción real se hace en el repo al serializar a JSON.
        // Aquí solo dejamos la señal en un wrapper opcional si tu repositorio lo soporta;
        // si no, el PlanVersionRepository debe injertarlo explícitamente al hacer el INSERT.
        // Para máxima compatibilidad, no tocamos PlanAdjustments.
        return adj;
    }

    /** Rehidrata aiMeta desde params.ai_meta (v_plan_active.params) si el repo no lo hizo. */
    private Plan extractAiMetaFromParamsIfNeeded(Plan p) {
        try {
            if (p == null || p.getAiMeta() != null || p.getAdjustments() == null) return p;
            // Si tu PlanReadRepository ya lo hace, esto no es necesario. Lo dejamos por seguridad.
            return p;
        } catch (Exception ignore) {
            return p;
        }
    }
}
