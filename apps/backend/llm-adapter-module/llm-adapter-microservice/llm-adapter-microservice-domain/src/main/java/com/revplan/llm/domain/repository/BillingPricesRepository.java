package com.revplan.llm.domain.repository;

import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import com.revplan.llm.domain.model.PriceCatalog;

import java.time.LocalDateTime;

public interface BillingPricesRepository {

    PriceCatalog fetchCatalog(ProviderEnum provider, TierEnum tier, String currency, boolean includeLegacy, LocalDateTime effectiveAt);
}
