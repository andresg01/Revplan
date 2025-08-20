package com.revapp.planengine.application.service.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

public final class KpiCalculator {
    // Precisión razonable y estable en prod
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    // Tolerancia para tratar sumas ~1.0 como 1.0 exacto (evita 0.99/1.01 por redondeos)
    private static final BigDecimal TOL = bd(0.005);

    private KpiCalculator(){}

    public static BigDecimal savingRate(BigDecimal savingPct) {
        return clamp01(nz(savingPct));
    }

    public static BigDecimal runwayMonthsScaled(BigDecimal runwayBase, BigDecimal savingBase, BigDecimal savingNew) {
        if (runwayBase == null) return bd(0);
        BigDecimal base = (savingBase == null || savingBase.compareTo(bd(1e-6)) <= 0) ? bd(1e-6) : savingBase;
        BigDecimal ratio = nz(savingNew).divide(base, MC);
        return max0(runwayBase.multiply(ratio, MC));
    }

    public static BigDecimal budgetCompliance(Map<String, BigDecimal> envelopes) {
        if (envelopes == null || envelopes.isEmpty()) return bd(0);
        BigDecimal sum = envelopes.values().stream()
                .filter(v -> v != null)
                .reduce(bd(0), BigDecimal::add);

        // tolerancia: si suma ~1.0 => 1.0
        BigDecimal diff = sum.subtract(bd(1), MC).abs();
        if (diff.compareTo(TOL) <= 0) return bd(1);

        BigDecimal raw = bd(1).subtract(diff, MC);
        return clamp01(raw);
    }

    // ---- helpers ----
    private static BigDecimal bd(double d) { return new BigDecimal(String.valueOf(d), MC); }
    private static BigDecimal nz(BigDecimal x){ return x == null ? bd(0) : x; }
    private static BigDecimal clamp01(BigDecimal x){
        if (x == null) return bd(0);
        if (x.compareTo(bd(0)) < 0) return bd(0);
        if (x.compareTo(bd(1)) > 0) return bd(1);
        return x.round(MC);
    }
    private static BigDecimal max0(BigDecimal x){ return (x == null || x.compareTo(bd(0)) < 0) ? bd(0) : x; }
}
