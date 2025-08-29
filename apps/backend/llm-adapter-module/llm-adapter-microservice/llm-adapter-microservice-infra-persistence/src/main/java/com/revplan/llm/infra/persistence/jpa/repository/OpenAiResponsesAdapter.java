package com.revplan.llm.infra.persistence.jpa.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplan.llm.domain.model.GenerationParams;
import com.revplan.llm.domain.model.GenerationResult;
import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.PlanLLMOutput;
import com.revplan.llm.domain.model.sse.SseChunkEvent;
import com.revplan.llm.domain.model.sse.SseFinishEvent;
import com.revplan.llm.domain.repository.PlanJsonProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * Adaptador a OpenAI Responses API (Structured Outputs) para generar el JSON del plan.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class OpenAiResponsesAdapter implements PlanJsonProviderRepository {

    private final @Qualifier("openAiWebClient") WebClient openAiWebClient;
    private final ObjectMapper om;

    @Value("${ai.systemPromptPath:classpath:prompts/financial-planner-system.txt}")
    private String systemPromptPath;

    // Cache de prompt
    private volatile String cachedSystemPrompt;
    private volatile String cachedPromptSource;
    private volatile String cachedPromptHash;
    private volatile int cachedPromptLen;

    /** JSON Schema oficial en memoria (equivalente a /schemas/plan). */
    private Map<String, Object> buildOfficialPlanSchema() {
        // ¡CORRECCIÓN FINAL! Schema explícito para 'envelopes'
        Map<String, Object> envelopeProperties = new LinkedHashMap<>();
        envelopeProperties.put("fixed", Map.of("type", "number", "minimum", 0, "maximum", 1));
        envelopeProperties.put("variable", Map.of("type", "number", "minimum", 0, "maximum", 1));
        envelopeProperties.put("goals", Map.of("type", "number", "minimum", 0, "maximum", 1));

        Map<String, Object> envelopesSchema = new LinkedHashMap<>();
        envelopesSchema.put("type", "object");
        envelopesSchema.put("properties", envelopeProperties);
        envelopesSchema.put("required", List.of("fixed", "variable", "goals"));
        envelopesSchema.put("additionalProperties", false);

        Map<String, Object> adjustmentsProps = new LinkedHashMap<>();
        adjustmentsProps.put("savingPct", Map.of("type", "number", "minimum", 0, "maximum", 1));
        adjustmentsProps.put("emergencyMonths", Map.of("type", "integer", "minimum", 0, "maximum", 24));
        adjustmentsProps.put("envelopes", envelopesSchema); // Usamos el schema explícito

        Map<String, Object> planAdj = new LinkedHashMap<>();
        planAdj.put("type", "object");
        planAdj.put("additionalProperties", false);
        planAdj.put("required", List.of("savingPct", "emergencyMonths", "envelopes"));
        planAdj.put("properties", adjustmentsProps);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("templateId", Map.of("type", "string", "minLength", 3, "maxLength", 64, "pattern", "^[A-Z0-9_]+$"));
        props.put("adjustments", planAdj);
        props.put("rationale", Map.of("type", "string", "minLength", 3, "maxLength", 2000));
        props.put("alerts", Map.of("type", "array", "items", Map.of("type", "string", "minLength", 1, "maxLength", 300)));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.put("required", List.of("templateId", "adjustments", "rationale", "alerts"));
        root.put("properties", props);
        return root;
    }

    /** Mensaje de usuario para el modelo. */
    private Map<String, Object> userMessageFrom(GeneratePlanJsonRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("templateId", req.getTemplateId()); // 'templateId' en camelCase
        m.put("variables", req.getVariables() != null ? req.getVariables() : Map.of());
        m.put("instructions", "Devuelve SOLO JSON válido para el JSON Schema. Sin texto extra.");
        return m;
    }

    @Override
    public GenerationResult generate(GeneratePlanJsonRequest request) {
        Instant t0 = Instant.now();

        String systemPrompt = ensureSystemPromptLoaded();
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = request.getSchema() instanceof Map<?, ?> m ? (Map<String, Object>) m : buildOfficialPlanSchema();

        var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", writeSafe(userMessageFrom(request)))
        );

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "PlanLLMOutput");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> payload = new LinkedHashMap<>();
        GenerationParams gen = request.getGeneration();
        String modelId = (gen != null && gen.getModelId() != null && !gen.getModelId().isBlank())
                ? gen.getModelId() : "gpt-5-mini";

        payload.put("model", modelId);
        payload.put("input", messages);
        payload.put("text", Map.of("format", format));

        if (gen != null) {
            if (gen.getMaxOutputTokens() != null) payload.put("max_output_tokens", gen.getMaxOutputTokens());
            if (gen.getSeed() != null)            payload.put("seed", gen.getSeed());
        }

        payload.put("reasoning", Map.of("effort", "low"));
        payload.put("store", false);
        payload.put("prompt_cache_key", "revplan-plan-json-schema-v1");

        if (log.isDebugEnabled()) {
            try {
                log.debug("Payload /responses:\n{}", om.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            } catch (JsonProcessingException ignore) {}
        }

        String raw = openAiWebClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class).defaultIfEmpty(r.statusCode().toString())
                                .flatMap(msg -> Mono.error(new RuntimeException("LLM error: " + msg))))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).jitter(0.25))
                .block();

        long latency = Duration.between(t0, Instant.now()).toMillis();

        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("LLM respuesta nula o vacía");
        }

        Map<?, ?> resp = safeRead(raw);
        Map<String, Object> structured = extractStructuredJson(resp);
        if (structured == null || structured.isEmpty()) {
            throw new RuntimeException("LLM sin JSON estructurado en output");
        }

        PlanLLMOutput out = om.convertValue(structured, PlanLLMOutput.class);
        String providerReqId = resp != null ? String.valueOf(resp.get("id")) : null;

        return GenerationResult.builder()
                .output(out)
                .providerRequestId(providerReqId)
                .durationMs((int) latency)
                .build();
    }

    @Override
    public void generateStream(GeneratePlanJsonRequest request,
                               Consumer<SseChunkEvent> onChunk,
                               Consumer<SseFinishEvent> onFinish) {
        GenerationResult res = generate(request);
        String json = writeSafe(res.getOutput());
        if (onChunk != null) onChunk.accept(SseChunkEvent.builder().delta(json).build());
        if (onFinish != null) onFinish.accept(SseFinishEvent.builder()
                .providerRequestId(res.getProviderRequestId())
                .durationMs(res.getDurationMs())
                .build());
    }

    // ================== Helpers ==================

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractStructuredJson(Map<?, ?> resp) {
        if (resp == null) return null;
        Object output = resp.get("output");
        if (output instanceof List<?> outList) {
            for (Object blockObj : outList) {
                if (!(blockObj instanceof Map<?, ?> block)) continue;
                if (!"message".equals(String.valueOf(block.get("type")))) continue;
                Object content = block.get("content");
                if (content instanceof List<?> cList) {
                    for (Object cObj : cList) {
                        if (!(cObj instanceof Map<?, ?> c)) continue;
                        if (c.get("structured") instanceof Map<?, ?> sMap) return (Map<String, Object>) sMap;
                        Map<String, Object> parsed = parseMaybeJson(c.get("text"));
                        if (parsed != null) return parsed;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMaybeJson(Object any) {
        try {
            if (any instanceof String s) {
                String trimmed = s.trim();
                if (trimmed.startsWith("{")) return om.readValue(trimmed, Map.class);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private Map<?, ?> safeRead(String json) {
        try { return om.readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }

    private String writeSafe(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    private String ensureSystemPromptLoaded() {
        if (cachedSystemPrompt != null) return cachedSystemPrompt;
        synchronized (this) {
            if (cachedSystemPrompt != null) return cachedSystemPrompt;
            try {
                byte[] bytes;
                String path = systemPromptPath;
                if (path.startsWith("classpath:")) {
                    ClassPathResource res = new ClassPathResource(path.substring("classpath:".length()));
                    bytes = res.getInputStream().readAllBytes();
                } else {
                    bytes = Files.readAllBytes(Paths.get(path));
                }
                cachedSystemPrompt = new String(bytes, StandardCharsets.UTF_8);
                log.info("SystemPrompt loaded [{}], size={}B", systemPromptPath, bytes.length);
                return cachedSystemPrompt;
            } catch (IOException e) {
                throw new RuntimeException("No se pudo leer system prompt: " + e.getMessage(), e);
            }
        }
    }
}