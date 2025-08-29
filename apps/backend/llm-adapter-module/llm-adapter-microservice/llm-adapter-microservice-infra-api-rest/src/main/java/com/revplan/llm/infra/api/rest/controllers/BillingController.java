package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.infra.api.dto.GetBillingPrices200ResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.UUID;

public class BillingController implements BillingApi{

    @Override
    public ResponseEntity<GetBillingPrices200ResponseDTO> getBillingPrices(UUID xRequestID, String acceptLanguage, String provider, String tier, String currency, Boolean includeLegacy, Date effectiveDate) {
        return BillingApi.super.getBillingPrices(xRequestID, acceptLanguage, provider, tier, currency, includeLegacy, effectiveDate);
    }
}
