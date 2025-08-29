package com.revapp.planengine.infra.persistence.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.AiMeta;
import com.revapp.planengine.domain.model.AiOptions;
import com.revapp.planengine.domain.model.AiResult;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.repository.PlanAiRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@Slf4j
public class LlmAdapterPlanRepositoryAdapter implements PlanAiRepository {

    private final WebClient llmClient;
    private final ObjectMapper om;
    private final int timeoutMs;

    private final String generatePath;
    private final String acceptLanguage;

    public LlmAdapterPlanRepositoryAdapter(
            @Qualifier("llmAdapterWebClient") WebClient llmClient,
            ObjectMapper om,
            @Value("${llm.adapter.timeoutMs:90000}") int timeoutMs,
            @Value("${llm.adapter.path.generate:/plans/generate}") String generatePath,
            @Value("${llm.adapter.acceptLanguage:es-ES}") String acceptLanguage
    ) {
        this.llmClient = llmClient;
        this.om = om;
        this.timeoutMs = timeoutMs;
        this.generatePath = generatePath;
        this.acceptLanguage = acceptLanguage;
    }


    @Override
    public AiResult generateFromBase(Plan base, String promptUser, AiOptions options) {
        try {
            // ---------- Payload que espera llm-adapter ----------
            Map<String, Object> generation = new LinkedHashMap<>();
            generation.put("modelId", options != null ? options.getModel() : null);
            if (options != null && options.getTemperature() != null) {
                generation.put("temperature", options.getTemperature());
            }
            generation.put("max_output_tokens", 1000);

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("prompt_user", promptUser);
            Map<String, Object> baseCompact = new LinkedHashMap<>();
            baseCompact.put("templateId", base.getTemplateId());
            baseCompact.put("adjustments", base.getAdjustments());
            baseCompact.put("kpis", base.getKpis());
            baseCompact.put("rationale", base.getRationale());
            baseCompact.put("alerts", base.getAlerts());
            variables.put("base", baseCompact);

            Map<String, Object> req = new LinkedHashMap<>();
            req.put("templateId", base.getTemplateId());
            req.put("variables", variables);
            req.put("generation", generation);

            if (log.isDebugEnabled()) {
                log.debug("llm-adapter request:\n{}", om.writerWithDefaultPrettyPrinter().writeValueAsString(req));
            }

            // ---------- Llamada HTTP (con headers requeridos) ----------
            Instant t0 = Instant.now();

            String raw = llmClient.post()
                    .uri(this.generatePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        h.add("X-Request-ID", UUID.randomUUID().toString());
                        h.add("Accept-Language", this.acceptLanguage);
                    })
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).defaultIfEmpty(r.statusCode().toString())
                                    .doOnNext(body -> log.warn("llm-adapter {} -> {}", r.statusCode(), body))
                                    .flatMap(msg -> Mono.error(new BusinessException(
                                            ErrorCode.BAD_GATEWAY, "llm-adapter error: " + msg))))
                    .bodyToMono(String.class)
                    .block(Duration.ofMillis(this.timeoutMs)); // <-- 3. Usar el campo inyectado

            long latency = Duration.between(t0, Instant.now()).toMillis();

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_GATEWAY, "llm-adapter respuesta vacía");
            }
            if (log.isDebugEnabled()) log.debug("llm-adapter raw response:\n{}", raw);

            // ---------- Parseo ----------
            Map<?, ?> resp = om.readValue(raw, Map.class);
            Object data = resp.get("data");
            if (data == null) data = resp;

            Plan patch = om.convertValue(data, Plan.class);

            AiMeta meta = AiMeta.builder()
                    .provider("llm-adapter")
                    .model(options != null ? options.getModel() : null)
                    .promptVersion(options != null ? options.getPromptVersion() : null)
                    .latencyMs((int) latency)
                    .build();

            log.info("LlmAdapterPlanRepositoryAdapter -> OK model='{}' latency={}ms",
                    options != null ? options.getModel() : null, latency);

            return AiResult.builder()
                    .planPatch(patch)
                    .meta(meta)
                    .build();

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("LlmAdapterPlanRepositoryAdapter.generateFromBase -> error", e);
            throw new BusinessException(ErrorCode.BAD_GATEWAY, "Error llamando a llm-adapter: " + e.getMessage());
        }
    }
}