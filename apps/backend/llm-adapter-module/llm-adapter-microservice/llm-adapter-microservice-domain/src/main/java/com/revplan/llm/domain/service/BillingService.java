package com.revplan.llm.domain.service;

import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import com.revplan.llm.domain.model.PriceCatalog;

import java.time.LocalDate;

public interface BillingService {

    PriceCatalog getPrices(ProviderEnum provider, TierEnum tier, String currency, boolean includeLegacy, LocalDate effectiveDate);
}
