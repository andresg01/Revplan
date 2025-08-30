package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import com.revplan.llm.domain.model.PriceCatalog;
import com.revplan.llm.domain.repository.BillingPricesRepository;
import com.revplan.llm.domain.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final BillingPricesRepository repository;

    @Override
    public PriceCatalog getPrices(ProviderEnum provider, TierEnum tier, String currency, boolean includeLegacy, LocalDate effectiveDate) {

        // effectiveAt: si no se indica, now(); si se indica, a las 00:00:00Z (UTC-less LocalDateTime)
        LocalDateTime effectiveAt = (effectiveDate == null) ? LocalDateTime.now() : effectiveDate.atStartOfDay();

        return repository.fetchCatalog(provider, tier, currency, includeLegacy, effectiveAt);
    }
}
