package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.service.ScorePreviewService;
import com.revapp.planengine.domain.utils.PageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScorePreviewServiceImpl implements ScorePreviewService {

    private final PlanTemplateRepository templateRepository;

    @Override
    public ScorePreviewData preview(ScorePreviewRequest request) {
        String nameLike = request != null ? request.getNameFilter() : null;
        boolean hasProfile = request != null && request.getProfile() != null;
        log.debug("ScorePreview.preview(nameFilter={}, hasProfile={})", nameLike, hasProfile);

        Instant t0 = Instant.now();
        var page = templateRepository.findAll(nameLike, PageRequest.of(0, 50));
        if (page == null) {
            log.error("ScorePreview.preview -> repository returned null page");
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Template page retrieval returned null");
        }

        List<ScorePreviewItem> items = new ArrayList<>();
        for (PlanTemplate t : page.items()) {
            var eval = evaluateTemplate(t, request);
            items.add(new ScorePreviewItem(t.getId(), eval.score(), eval.factors()));
        }

        log.info("ScorePreview.preview(nameFilter={}) -> {} items in {}", nameLike, items.size(), Duration.between(t0, Instant.now()));
        return new ScorePreviewData(items);
    }

    /** Resultado interno: score + mapa de factores. */
    private record Eval(BigDecimal score, Map<String, BigDecimal> factors) {}

    /** Evalúa una plantilla con pesos (0..1) contra el snapshot del request (si existe). */
    private Eval evaluateTemplate(PlanTemplate tpl, ScorePreviewRequest req) {
        TemplateWeights w = tpl.getWeights();

        // Defaults cuando falten pesos (robusto)
        BigDecimal wr = nz(w != null ? w.getRisk()       : null);
        BigDecimal wl = nz(w != null ? w.getLiquidity()  : null);
        BigDecimal wd = nz(w != null ? w.getDebt()       : null);
        BigDecimal wg = nz(w != null ? w.getSavingsGap() : null);
        BigDecimal wa = nz(w != null ? w.getAge()        : null);
        BigDecimal ws = nz(w != null ? w.getStability()  : null);

        // --- Features normalizados 0..1 desde el request (con defaults si faltan)
        ProfileSnapshot p = req != null ? req.getProfile() : null;
        Double riskNorm       = p != null && p.getRiskScore()     != null ? clamp01((p.getRiskScore() - 1) / 4.0) : 0.5;
        Double liquidityNorm  = p != null && p.getLiquidityNeed() != null ? clamp01(p.getLiquidityNeed().doubleValue()) : 0.5;
        Double debtNorm       = p != null && p.getDebtSeverity()  != null ? clamp01(p.getDebtSeverity().doubleValue())  : 0.0;
        Double stabilityNorm  = p != null && p.getStabilityIndex()!= null ? clamp01(p.getStabilityIndex().doubleValue()): 0.5;
        Double ageNorm        = p != null && p.getAge()           != null ? clamp01((p.getAge() - 16.0) / (100.0 - 16.0)) : 0.5;

        // savingsGap: si tengo ingresos y gastos, gap respecto a target 25% (simple)
        Double savingsGapNorm = 0.5;
        if (p != null && p.getIncomeNet() != null && p.getExpensesFixed() != null && p.getExpensesVariable() != null) {
            double income = Math.max(0.0, p.getIncomeNet().doubleValue());
            if (income > 0) {
                double out = Math.max(0.0, p.getExpensesFixed().doubleValue()) + Math.max(0.0, p.getExpensesVariable().doubleValue());
                double actualSavingRate = clamp01((income - out) / income);
                double target = 0.25; // baseline; puedes mejorarlo leyendo constraints del template si quieres
                double gap = Math.max(0.0, target - actualSavingRate) / target; // 0 si ya cumples
                savingsGapNorm = clamp01(gap);
            } else {
                savingsGapNorm = 1.0; // sin ingresos -> máximo gap
            }
        }

        // --- Contribuciones por dimensión (peso * feature)
        Map<String, BigDecimal> factors = new LinkedHashMap<>();
        BigDecimal cRisk      = wr.multiply(bd(riskNorm));
        BigDecimal cLiquidity = wl.multiply(bd(liquidityNorm));
        BigDecimal cDebt      = wd.multiply(bd(debtNorm));
        BigDecimal cGap       = wg.multiply(bd(savingsGapNorm));
        BigDecimal cAge       = wa.multiply(bd(ageNorm));
        BigDecimal cStability = ws.multiply(bd(stabilityNorm));

        factors.put("risk",        scale4(cRisk));
        factors.put("liquidity",   scale4(cLiquidity));
        factors.put("debt",        scale4(cDebt));
        factors.put("savingsGap",  scale4(cGap));
        factors.put("age",         scale4(cAge));
        factors.put("stability",   scale4(cStability));

        // --- Score: suma de contribuciones (si todo cero, usa 0.5)
        BigDecimal sum = cRisk.add(cLiquidity).add(cDebt).add(cGap).add(cAge).add(cStability);
        BigDecimal score = sum.compareTo(BigDecimal.ZERO) == 0 ? bd(0.5) : scale4(sum);

        return new Eval(score, factors);
    }

    private static BigDecimal nz(BigDecimal x) { return x != null ? x : BigDecimal.ZERO; }
    private static BigDecimal bd(double d) { return new BigDecimal(String.valueOf(d)); }
    private static BigDecimal scale4(BigDecimal x) { return x.setScale(4, RoundingMode.HALF_UP); }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
