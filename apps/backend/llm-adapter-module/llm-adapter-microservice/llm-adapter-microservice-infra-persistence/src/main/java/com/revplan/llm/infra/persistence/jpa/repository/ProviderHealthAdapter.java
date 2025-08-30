package com.revplan.llm.infra.persistence.jpa.repository;

import com.revplan.llm.domain.model.HealthLLM;
import com.revplan.llm.domain.repository.HealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;


@Component
@RequiredArgsConstructor
@Slf4j
public class ProviderHealthAdapter implements HealthRepository {

    private final @Qualifier("openAiWebClient") WebClient openAiWebClient;

    @Override
    public HealthLLM check() {
        boolean reachable = false;
        Integer latencyMs = null;
        Instant t0 = Instant.now();

        try {
            String body = openAiWebClient.get()
                    .uri("/models")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty(r.statusCode().toString())
                                    .doOnNext(msg -> log.warn("Health /models {} -> {}", r.statusCode(), msg))
                                    .flatMap(msg -> Mono.error(new RuntimeException("Provider error: " + msg)))
                    )
                    .bodyToMono(String.class)
                    .block();

            latencyMs = (int) Duration.between(t0, Instant.now()).toMillis();
            reachable = (body != null && !body.isBlank());
        } catch (Exception e) {
            log.warn("Provider health check failed: {}", e.toString());
            reachable = false;
            latencyMs = null;
        }

        return HealthLLM.builder()
                .provider("openai")
                .reachable(reachable)
                .modelLatencyMs(latencyMs)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
