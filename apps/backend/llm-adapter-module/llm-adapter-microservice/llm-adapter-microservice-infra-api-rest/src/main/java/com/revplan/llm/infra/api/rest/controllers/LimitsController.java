package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.domain.model.Limits;
import com.revplan.llm.domain.service.LimitsService;
import com.revplan.llm.infra.api.dto.GetLimits200ResponseDTO;
import com.revplan.llm.infra.api.rest.mapper.LimitsApiMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LimitsController implements LimitsApi {

    private final LimitsService limitsService;
    private final LimitsApiMapper mapper;
    private final HttpServletRequest request;

    @Override
    public ResponseEntity<GetLimits200ResponseDTO> getLimits() {
        Limits limits = limitsService.getLimits();
        GetLimits200ResponseDTO body = mapper.toDto(limits);

        int limit = nvl(limits.getLimitPerMinute(), 0);
        int usage = Math.max(0, nvl(limits.getEstimatedUsageMinute(), 0));
        int remaining = Math.max(0, limit - usage);
        long resetEpoch = computeNextMinuteResetEpochSeconds();

        String requestId = headerOrGenerate("X-Request-ID");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(limit));
        headers.add("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.add("X-RateLimit-Reset", String.valueOf(resetEpoch));
        headers.add("X-Request-ID", requestId);

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    private long computeNextMinuteResetEpochSeconds() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nextMinute = now.plusMinutes(1).withSecond(0).withNano(0);
        return nextMinute.toEpochSecond();
    }

    private String headerOrGenerate(String name) {
        String v = request.getHeader(name);
        return (v == null || v.isBlank()) ? UUID.randomUUID().toString() : v;
    }

    private int nvl(Integer v, int def) {
        return v == null ? def : v;
    }
}
