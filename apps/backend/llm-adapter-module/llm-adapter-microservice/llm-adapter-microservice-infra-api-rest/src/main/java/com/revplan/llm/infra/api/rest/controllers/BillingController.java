package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.domain.enums.ProviderEnum;
import com.revplan.llm.domain.enums.TierEnum;
import com.revplan.llm.domain.model.PriceCatalog;
import com.revplan.llm.domain.service.BillingService;
import com.revplan.llm.infra.api.dto.GetBillingPrices200ResponseDTO;
import com.revplan.llm.infra.api.rest.mapper.BillingPricesApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BillingController implements BillingApi {

    private final BillingService billingService;
    private final BillingPricesApiMapper mapper;

    @Override
    public ResponseEntity<GetBillingPrices200ResponseDTO> getBillingPrices(
            UUID xRequestID,
            String acceptLanguage,
            String provider,
            String tier,
            String currency,
            Boolean includeLegacy,
            Date effectiveDate
    ) {
        ProviderEnum providerEnum = parseProvider(provider);
        TierEnum tierEnum = parseTier(tier);
        String curr = (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
        boolean includeLegacyBool = includeLegacy == null ? true : includeLegacy;

        LocalDate effective = null;
        if (effectiveDate != null) {
            effective = effectiveDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }

        PriceCatalog catalog = billingService.getPrices(providerEnum, tierEnum, curr, includeLegacyBool, effective);
        GetBillingPrices200ResponseDTO dto = mapper.toDtoResponse(catalog);

        String reqId = xRequestID != null ? xRequestID.toString() : UUID.randomUUID().toString();
        return ResponseEntity.ok()
                .header("X-Request-ID", reqId)
                .body(dto);
    }

    private ProviderEnum parseProvider(String p) {
        if (p == null || p.isBlank()) return ProviderEnum.OPENAI;
        try {
            return ProviderEnum.valueOf(p.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProviderEnum.OPENAI; // fallback seguro
        }
    }

    private TierEnum parseTier(String t) {
        if (t == null || t.isBlank()) return TierEnum.STANDARD;
        try {
            return TierEnum.valueOf(t.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TierEnum.STANDARD; // fallback seguro
        }
    }
}
