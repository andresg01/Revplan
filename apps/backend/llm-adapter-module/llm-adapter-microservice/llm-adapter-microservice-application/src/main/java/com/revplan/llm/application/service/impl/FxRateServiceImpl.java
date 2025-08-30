package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.repository.FxRateRepository;
import com.revplan.llm.domain.service.FxRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
public class FxRateServiceImpl implements FxRateService {

    private final FxRateRepository fxRateRepository;

    private static class CachedRate {
        final BigDecimal rate;
        final long ts;
        CachedRate(BigDecimal r, long t) { this.rate = r; this.ts = t; }
    }

    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 30 * 60 * 1000; // 30 minutos

    @Override
    public BigDecimal getRate(String from, String to) {
        String f = from == null ? "USD" : from.toUpperCase();
        String t = to == null ? "USD" : to.toUpperCase();
        if (f.equals(t)) return BigDecimal.ONE;

        String key = f + "->" + t;
        long now = Instant.now().toEpochMilli();
        CachedRate cr = cache.get(key);
        if (cr != null && (now - cr.ts) < TTL_MS) {
            return cr.rate;
        }

        BigDecimal fresh = fxRateRepository.getRate(f, t);
        cache.put(key, new CachedRate(fresh, now));
        return fresh;
    }
}
