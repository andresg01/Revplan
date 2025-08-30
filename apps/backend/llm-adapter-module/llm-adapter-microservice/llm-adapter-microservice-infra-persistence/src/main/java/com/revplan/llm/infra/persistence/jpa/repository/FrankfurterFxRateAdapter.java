package com.revplan.llm.infra.persistence.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplan.llm.domain.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;


@Repository
@RequiredArgsConstructor
@Slf4j
public class FrankfurterFxRateAdapter implements FxRateRepository {

    private final @Qualifier("fxWebClient") WebClient fxWebClient;
    private final ObjectMapper om;

    @Override
    public BigDecimal getRate(String from, String to) {
        String f = (from == null ? "USD" : from.toUpperCase());
        String t = (to == null ? "USD" : to.toUpperCase());
        if (f.equals(t)) return BigDecimal.ONE;

        // Ejemplo: /latest?from=USD&to=EUR
        String uri = String.format("/latest?from=%s&to=%s", f, t);

        String raw = fxWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class).defaultIfEmpty(r.statusCode().toString())
                                .flatMap(msg -> Mono.error(new RuntimeException("FX error: " + msg))))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).jitter(0.25))
                .block();

        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("FX respuesta nula/vacía");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = om.readValue(raw, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) map.get("rates");
            if (rates == null || !rates.containsKey(t)) {
                throw new RuntimeException("FX sin campo 'rates' para " + t);
            }
            Object val = rates.get(t);
            BigDecimal rate = new BigDecimal(String.valueOf(val));
            log.debug("FX {}->{} rate={}", f, t, rate);
            return rate;
        } catch (Exception e) {
            throw new RuntimeException("FX parse error: " + e.getMessage(), e);
        }
    }
}
