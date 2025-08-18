package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.model.ScorePreviewData;
import com.revapp.planengine.domain.model.ScorePreviewItem;
import com.revapp.planengine.domain.model.ScorePreviewRequest;
import com.revapp.planengine.domain.model.TemplateWeights;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.service.ScorePreviewService;
import com.revapp.planengine.domain.utils.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScorePreviewServiceImpl implements ScorePreviewService {

    private final PlanTemplateRepository templateRepository;

    @Override
    public ScorePreviewData preview(ScorePreviewRequest request) {
        String nameLike = request != null ? request.getNameFilter() : null;

        var page = templateRepository.findAll(nameLike, PageRequest.of(0, 50));
        if (page == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Template page retrieval returned null");
        }

        List<ScorePreviewItem> items = new ArrayList<>();
        for (PlanTemplate t : page.items()) {
            BigDecimal score = computeScoreFromWeights(t.getWeights());
            items.add(new ScorePreviewItem(t.getId(), score, null));
        }

        return new ScorePreviewData(items);
    }

    /** Score determinista a partir de los pesos; media de presentes (0..1). */
    private BigDecimal computeScoreFromWeights(TemplateWeights w) {
        if (w == null) return bd(0.5);

        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;

        if (w.getRisk() != null)        { sum = sum.add(clamp01(w.getRisk()));        n++; }
        if (w.getLiquidity() != null)   { sum = sum.add(clamp01(w.getLiquidity()));   n++; }
        if (w.getDebt() != null)        { sum = sum.add(clamp01(w.getDebt()));        n++; }
        if (w.getSavingsGap() != null)  { sum = sum.add(clamp01(w.getSavingsGap()));  n++; }
        if (w.getAge() != null)         { sum = sum.add(clamp01(w.getAge()));         n++; }
        if (w.getStability() != null)   { sum = sum.add(clamp01(w.getStability()));   n++; }

        if (n == 0) return bd(0.5);
        return sum.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(double d) {
        return new BigDecimal(String.valueOf(d));
    }

    private static BigDecimal clamp01(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (x.compareTo(BigDecimal.ONE)  > 0) return BigDecimal.ONE;
        return x;
    }
}
