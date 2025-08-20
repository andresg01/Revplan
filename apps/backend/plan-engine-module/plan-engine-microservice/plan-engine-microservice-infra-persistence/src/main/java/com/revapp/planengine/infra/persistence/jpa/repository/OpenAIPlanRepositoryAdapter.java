package com.revapp.planengine.infra.persistence.jpa.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.domain.repository.PlanAiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

@Repository
@RequiredArgsConstructor
@Slf4j
public class OpenAIPlanRepositoryAdapter implements PlanAiRepository {

    private final @Qualifier("openAiWebClient") WebClient openAiWebClient;
    private final ObjectMapper om;

    @Value("${ai.systemPromptPath:classpath:prompts/financial-planner-system.txt}")
    private String systemPromptPath;

    // Cache prompt sistema
    private volatile String cachedSystemPrompt;
    private volatile String cachedPromptSource;
    private volatile String cachedPromptHash;
    private volatile int cachedPromptLen;

    @Override
    public AiResult generateFromBase(Plan base, String promptUser, AiOptions options) {
        log.info("Iniciando generación de plan para prompt: '{}...'", snippet(promptUser, 80));
        try {
            String systemPrompt = ensureSystemPromptLoaded();

            // ====== JSON Schema: PlanPatch ======
            Map<String, Object> envelopeItemProps = new LinkedHashMap<>();
            envelopeItemProps.put("category", Map.of("type", "string", "minLength", 1));
            envelopeItemProps.put("percentage", Map.of("type", "number", "minimum", 0, "maximum", 1));

            Map<String, Object> envelopeItemSchema = new LinkedHashMap<>();
            envelopeItemSchema.put("type", "object");
            envelopeItemSchema.put("additionalProperties", false);
            envelopeItemSchema.put("required", List.of("category", "percentage"));
            envelopeItemSchema.put("properties", envelopeItemProps);

            Map<String, Object> envelopesArraySchema = new LinkedHashMap<>();
            envelopesArraySchema.put("type", "array");
            envelopesArraySchema.put("items", envelopeItemSchema);
            envelopesArraySchema.put("minItems", 0);

            Map<String, Object> adjustmentsProps = new LinkedHashMap<>();
            adjustmentsProps.put("savingPct", Map.of("type", "number", "minimum", 0, "maximum", 1));
            adjustmentsProps.put("emergencyMonths", Map.of("type", "integer", "minimum", 0, "maximum", 24));
            adjustmentsProps.put("envelopes", envelopesArraySchema);

            Map<String, Object> adjustmentsSchema = new LinkedHashMap<>();
            adjustmentsSchema.put("type", "object");
            adjustmentsSchema.put("additionalProperties", false);
            adjustmentsSchema.put("required", List.of("savingPct", "emergencyMonths", "envelopes"));
            adjustmentsSchema.put("properties", adjustmentsProps);

            Map<String, Object> kpisProps = new LinkedHashMap<>();
            kpisProps.put("savingRate", Map.of("type", "number", "minimum", 0, "maximum", 1));
            kpisProps.put("runwayMonths", Map.of("type", "number", "minimum", 0, "maximum", 480));
            kpisProps.put("budgetCompliance", Map.of("type", "number", "minimum", 0, "maximum", 1));

            Map<String, Object> kpisSchema = new LinkedHashMap<>();
            kpisSchema.put("type", "object");
            kpisSchema.put("additionalProperties", false);
            kpisSchema.put("required", List.of("savingRate", "runwayMonths", "budgetCompliance"));
            kpisSchema.put("properties", kpisProps);

            Map<String, Object> rootProps = new LinkedHashMap<>();
            rootProps.put("rationale", Map.of("type", "string"));
            rootProps.put("alerts", Map.of("type", "array", "items", Map.of("type", "string")));
            rootProps.put("adjustments", adjustmentsSchema);
            rootProps.put("kpis", kpisSchema);

            Map<String, Object> rootSchema = new LinkedHashMap<>();
            rootSchema.put("type", "object");
            rootSchema.put("additionalProperties", false);
            rootSchema.put("required", List.of("rationale", "alerts", "adjustments", "kpis"));
            rootSchema.put("properties", rootProps);
            // ====== FIN schema ======

            var messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", promptUser)
            );

            // ===== CORRECT PAYLOAD STRUCTURE FOR /v1/responses API =====
            // The error indicates 'response_format' is outdated. The correct parameter
            // is 'text.format' for the Responses API.
            var textFormat = new LinkedHashMap<String, Object>();
            textFormat.put("type", "json_schema");
            textFormat.put("name", "PlanPatch");
            textFormat.put("schema", rootSchema);
            textFormat.put("strict", true);

            var payload = new LinkedHashMap<String, Object>();
            payload.put("model", options.getModel());
            payload.put("input", messages);
            payload.put("text", Map.of("format", textFormat)); // <-- CORRECTED PARAMETER

            // Cost/latency optimizations
            payload.put("reasoning", Map.of("effort", "low"));
            payload.put("max_output_tokens", 1000); // Increased limit

            // Optional: Token saving and privacy
            payload.put("prompt_cache_key", "revplan-financial-planner-v2025-08-19");
            payload.put("store", false);

            if (log.isDebugEnabled()) {
                try {
                    log.debug("Enviando payload a OpenAI:\n{}", om.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
                } catch (JsonProcessingException e) {
                    log.debug("Enviando payload a OpenAI (no serializable): {}", payload);
                }
            }

            Instant t0 = Instant.now();

            String rawResponse = openAiWebClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class).defaultIfEmpty(r.statusCode().toString())
                                    .doOnNext(body -> log.warn("Error {} de la API LLM. Body: {}", r.statusCode(), body))
                                    .flatMap(msg -> Mono.error(new BusinessException(
                                            ErrorCode.BAD_GATEWAY, "LLM error: " + msg))))
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).jitter(0.25)
                            .filter(ex -> ex instanceof WebClientRequestException ||
                                    ex instanceof WebClientResponseException.TooManyRequests ||
                                    ex instanceof WebClientResponseException.InternalServerError)
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .block();

            long latency = Duration.between(t0, Instant.now()).toMillis();
            log.debug("Respuesta recibida de OpenAI (raw):\n{}", rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM respuesta nula o vacía");
            }

            Map<?, ?> resp = om.readValue(rawResponse, Map.class);

            Map<String, Object> structured = extractStructuredJson(resp);
            if (structured == null) {
                log.warn("No se encontró 'structured' JSON en la respuesta. Respuesta completa de la API:\n{}", rawResponse);
                throw new BusinessException(ErrorCode.BAD_GATEWAY, "LLM sin JSON en output");
            }

            // ====== POST-PROCESSING: envelopes array -> map<String, Double> ======
            try {
                Object adjustmentsObj = structured.get("adjustments");
                if (adjustmentsObj instanceof Map<?, ?> adjAny) {
                    Map<String, Object> adjustments = castMap(adjAny);
                    Object envelopesObj = adjustments.get("envelopes");
                    if (envelopesObj instanceof List<?> list) {
                        Map<String, Double> envMap = new LinkedHashMap<>();
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> it) {
                                Object cat = it.get("category");
                                Object pct = it.get("percentage");
                                if (cat != null && pct != null) {
                                    envMap.put(String.valueOf(cat), toDouble(pct));
                                }
                            }
                        }
                        // Normalize to 1.0 (protect against rounding errors)
                        double sum = envMap.values().stream()
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum();
                        if (sum > 0 && Math.abs(sum - 1.0) > 1e-6) {
                            final double k = 1.0 / sum;
                            envMap.replaceAll((kCat, v) -> clamp01((v == null ? 0.0 : v) * k));
                        }
                        adjustments.put("envelopes", envMap);
                    }
                }
            } catch (Exception e) {
                log.warn("Postproceso envelopes -> map falló: {}", e.toString());
            }

            Plan patch = om.convertValue(structured, Plan.class);

            // Token telemetry
            Integer tp = null, to = null;
            Object usage = resp.get("usage");
            if (usage instanceof Map<?, ?> u) {
                tp = asInt(u.get("input_tokens"));
                to = asInt(u.get("output_tokens"));
            }

            AiMeta meta = AiMeta.builder()
                    .provider("openai")
                    .model(options.getModel())
                    .promptVersion(options.getPromptVersion())
                    .tokensPrompt(tp)
                    .tokensOutput(to)
                    .latencyMs((int) latency)
                    .build();

            log.info("OpenAIPlanRepositoryAdapter -> OK model='{}' latency={}ms tokens(in={}, out={})",
                    options.getModel(), latency, tp, to);

            return AiResult.builder().planPatch(patch).meta(meta).build();

        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("OpenAIPlanRepositoryAdapter.generateFromBase -> error", ex);
            throw new BusinessException(ErrorCode.BAD_GATEWAY, "Adapter LLM error: " + ex.getMessage());
        }
    }

    // ================== Helpers ==================

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractStructuredJson(Map<?, ?> resp) {
        Object output = resp.get("output");
        if (output instanceof List<?> outList) {
            for (Object blockObj : outList) {
                if (blockObj instanceof Map<?, ?> block) {
                    Object type = block.get("type"); // "message" or "reasoning"
                    if (!"message".equals(String.valueOf(type))) continue;

                    Object content = block.get("content");
                    if (content instanceof List<?> cList) {
                        for (Object cObj : cList) {
                            if (cObj instanceof Map<?, ?> c) {
                                // 1) structured directo (cuando response_format funciona)
                                Object s = c.get("structured");
                                if (s instanceof Map<?, ?> sMap) return castMap(sMap);

                                // 2) texto con JSON (caso habitual en mini/nano)
                                Object t = c.get("text");
                                Map<String, Object> parsed = parseMaybeJson(t);
                                if (parsed != null) return parsed;

                                // 3) a veces viene en "value"
                                Object v = c.get("value");
                                parsed = parseMaybeJson(v);
                                if (parsed != null) return parsed;

                                // 4) tool call (si activas tools en el futuro)
                                Object ctype = c.get("type");
                                if ("tool_call".equals(String.valueOf(ctype))) {
                                    Object args = c.get("arguments");
                                    parsed = parseMaybeJson(args);
                                    if (parsed != null) return parsed;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMaybeJson(Object textObj) {
        try {
            String s = extractText(textObj);
            if (s == null || s.isBlank()) return null;

            s = s.trim();
            if (!(s.startsWith("{") || s.startsWith("["))) {
                int i = s.indexOf('{');
                int j = s.lastIndexOf('}');
                if (i >= 0 && j > i) s = s.substring(i, j + 1);
                else return null;
            }

            try {
                return om.readValue(s, Map.class);
            } catch (Exception ignore) {
                List<Object> arr = om.readValue(s, List.class);
                Map<String, Object> wrapped = new LinkedHashMap<>();
                wrapped.put("items", arr);
                return wrapped;
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Object textObj) {
        if (textObj == null) return null;
        if (textObj instanceof String s) return s;
        if (textObj instanceof Map<?, ?> m) {
            Object v = m.get("value");
            return (v instanceof String) ? (String) v : null;
        }
        if (textObj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object o : list) {
                if (o instanceof String s) sb.append(s);
                else if (o instanceof Map<?, ?> m2) {
                    Object v2 = m2.get("value");
                    if (v2 instanceof String vs2) sb.append(vs2);
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return null;
    }

    private static Double toDouble(Object o) {
        try {
            if (o instanceof Number n) return n.doubleValue();
            if (o != null) return Double.parseDouble(String.valueOf(o));
        } catch (Exception ignore) {}
        return null;
    }

    private static double clamp01(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    private String ensureSystemPromptLoaded() {
        if (cachedSystemPrompt != null) return cachedSystemPrompt;
        synchronized (this) {
            if (cachedSystemPrompt != null) return cachedSystemPrompt;
            try {
                byte[] bytes;
                String path = systemPromptPath;
                if (path.startsWith("classpath:")) {
                    String cp = path.substring("classpath:".length());
                    ClassPathResource res = new ClassPathResource(cp);
                    if (!res.exists()) {
                        log.error("SystemPrompt no encontrado en CLASSPATH: '{}'", cp);
                        throw new BusinessException(ErrorCode.BAD_GATEWAY,
                                "System prompt no disponible en classpath: " + cp);
                    }
                    bytes = res.getInputStream().readAllBytes();
                    cachedPromptSource = "CLASSPATH:" + cp;
                } else {
                    Path p = path.startsWith("file:") ? Paths.get(URI.create(path)) : Paths.get(path);
                    if (!Files.exists(p)) {
                        log.error("systemPromptPath no existe como FILE: '{}'", p.toAbsolutePath());
                        throw new BusinessException(ErrorCode.BAD_GATEWAY,
                                "System prompt no disponible en file: " + p.toAbsolutePath());
                    }
                    bytes = Files.readAllBytes(p);
                    cachedPromptSource = "FILE:" + p.toAbsolutePath();
                }
                cachedSystemPrompt = new String(bytes, StandardCharsets.UTF_8);
                cachedPromptLen = bytes.length;
                cachedPromptHash = sha256Hex(bytes);
                log.info("SystemPrompt loaded [{}], size={}B, sha256={}",
                        cachedPromptSource, cachedPromptLen, cachedPromptHash);
                if (log.isDebugEnabled()) log.debug("SystemPrompt (head 200): {}", snippet(cachedSystemPrompt, 200));
                return cachedSystemPrompt;
            } catch (IOException e) {
                log.error("Error leyendo systemPrompt '{}': {}", systemPromptPath, e.toString());
                throw new BusinessException(ErrorCode.BAD_GATEWAY, "No se pudo leer system prompt: " + e.getMessage());
            }
        }
    }

    private static String snippet(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "na";
        }
    }
}