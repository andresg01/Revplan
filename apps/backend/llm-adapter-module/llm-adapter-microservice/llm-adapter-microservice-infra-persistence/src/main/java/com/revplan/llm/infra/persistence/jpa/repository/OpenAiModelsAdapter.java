package com.revplan.llm.infra.persistence.jpa.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplan.llm.domain.enums.ModelModalityEnum;
import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.enums.ModelStatusEnum;
import com.revplan.llm.domain.model.ModelSpec;
import com.revplan.llm.domain.repository.ModelsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter de modelos para OpenAI:
 *  - Devuelve modelos de texto/razonamiento alineados con el OpenAPI:
 *      modalidades = [TEXT, STRUCTURED_JSON]
 *      status      = ACTIVE | DEPRECATED | UNAVAILABLE
 *      contextWindow y maxOutputTokens siempre informados (rangos del contrato).
 *  - Intenta consultar /models del provider y fusiona con un snapshot curado.
 */
@Repository
@Slf4j
public class OpenAiModelsAdapter implements ModelsRepository {

    private static final List<ModelModalityEnum> DEFAULT_MODALITIES =
            List.of(ModelModalityEnum.TEXT, ModelModalityEnum.STRUCTURED_JSON);

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiModelsAdapter(@Qualifier("openAiWebClient") WebClient openAiWebClient,
                               ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------------------------------
    @Override
    public List<ModelSpec> fetchModels(ModelProviderEnum provider) {
        ModelProviderEnum prov = (provider == null ? ModelProviderEnum.OPENAI : provider);
        if (prov != ModelProviderEnum.OPENAI) {
            return List.of(); // extensible a otros providers
        }

        // 1) Snapshot curado (siempre disponible)
        Map<String, ModelSpec> curated = curatedSnapshot()
                .stream().collect(Collectors.toMap(ModelSpec::getId, m -> m, (a, b) -> a, LinkedHashMap::new));

        // 2) Intento remoto /models
        Map<String, ModelSpec> remote = Collections.emptyMap();
        try {
            String raw = openAiWebClient.get()
                    .uri("/models")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty(r.statusCode().toString())
                                    .flatMap(msg -> Mono.error(new RuntimeException("OpenAI /models error: " + msg))))
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(1)).jitter(0.25))
                    .block();

            if (raw != null && !raw.isBlank()) {
                Map<?, ?> parsed = objectMapper.readValue(raw, Map.class);
                Object data = parsed.get("data");
                if (data instanceof List<?> list) {
                    List<ModelSpec> mapped = list.stream()
                            .map(this::mapOpenAiItem)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    remote = mapped.stream().collect(Collectors.toMap(ModelSpec::getId, m -> m, (a, b) -> a, LinkedHashMap::new));
                }
            }
        } catch (Exception ex) {
            log.debug("Fallo consultando OpenAI /models: {}. Continuamos con snapshot curado + heurística.", ex.toString());
        }

        // 3) Fusión: prioridad al snapshot curado (estados/ventanas “buenas” para tu caso)
        LinkedHashMap<String, ModelSpec> merged = new LinkedHashMap<>();
        merged.putAll(remote);   // primero remoto
        merged.putAll(curated);  // el curado pisa remoto en conflicto

        // 4) Orden estable por id
        return merged.values().stream()
                .sorted(Comparator.comparing(ModelSpec::getId))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------
    // Mapeo /models (robusto y minimalista para nuestro contrato)
    // ------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ModelSpec mapOpenAiItem(Object item) {
        try {
            if (!(item instanceof Map<?, ?> m)) return null;
            String id = String.valueOf(m.get("id"));
            if (id == null || id.isBlank()) return null;

            // Filtramos cosas que claramente no son chat/texto (embeddings, vision-only, imagen/tts…)
            if (!looksLikeTextModel(id)) return null;

            return ModelSpec.builder()
                    .id(id)
                    .provider(ModelProviderEnum.OPENAI)
                    .modalities(DEFAULT_MODALITIES)
                    .status(guessStatus(id))
                    .contextWindow(guessContextWindow(id))
                    .maxOutputTokens(guessMaxOutputTokens(id))
                    .build();

        } catch (Exception e) {
            log.debug("Ignorando item /models por parseo: {}", e.toString());
            return null;
        }
    }

    private boolean looksLikeTextModel(String id) {
        String s = id.toLowerCase(Locale.ROOT);
        // Excluir explícitos no-texto
        if (s.contains("embedding") || s.contains("image") || s.contains("dall") ||
                s.contains("whisper") || s.startsWith("tts") || s.contains("moderation") ||
                s.contains("transcribe") || s.contains("tts") || s.contains("audio"))
            return false;

        // Preferimos familias conocidas de texto/razonamiento/código/chat
        return s.startsWith("gpt-") || s.startsWith("o") || s.contains("codex") || s.contains("oss") ||
                s.contains("computer-use") || s.contains("search-preview") || s.contains("chatgpt");
    }

    // ------------------------------------------------------------------------------------------------
    // Heurísticas de campos requeridos (acotadas a los rangos del OpenAPI)
    // ------------------------------------------------------------------------------------------------
    private ModelStatusEnum guessStatus(String id) {
        String s = id.toLowerCase(Locale.ROOT);

        // Marcamos deprecated si el propio nombre lo sugiere o si son generaciones antiguas
        if (s.contains("deprecated") || s.contains("preview") || s.contains("legacy") ||
                s.contains("0314") || s.contains("0613") || s.contains("1106") || s.contains("0125") ||
                s.contains("3.5") || s.contains("4-") || s.contains("4-turbo") ||
                s.contains("chatgpt-4o-latest") || s.contains("gpt-4o-2024-05-13") ||
                s.contains("4.5-preview") || s.contains("o1-mini") || s.contains("o1-preview"))
            return ModelStatusEnum.DEPRECATED;

        // Si aparece algún indicio de baja temporal
        if (s.contains("unavailable") || s.contains("disabled")) {
            return ModelStatusEnum.UNAVAILABLE;
        }
        return ModelStatusEnum.ACTIVE;
    }

    private Integer guessContextWindow(String id) {
        String s = id.toLowerCase(Locale.ROOT);

        // Límites del contrato: [4096 .. 1048576]
        if (s.startsWith("gpt-5") || s.startsWith("o3-pro") || s.startsWith("o1-pro")) return 200_000;
        if (s.contains("4o") || s.contains("4.1") || s.contains("o3") || s.contains("o4-mini")) return 128_000;
        if (s.contains("codex") || s.contains("oss")) return 128_000;

        if (s.startsWith("gpt-4-32k")) return 32_768;
        if (s.startsWith("gpt-4-")) return 8_192;

        if (s.contains("3.5")) {
            if (s.contains("16k")) return 16_384;
            return 8_192; // algunas 3.5 tenían 4k/16k; usamos valor seguro
        }
        if (s.contains("chatgpt-4o-latest") || s.contains("gpt-5-chat-latest")) return 128_000;

        // Por defecto, un valor razonable y válido
        return 128_000;
    }

    private Integer guessMaxOutputTokens(String id) {
        String s = id.toLowerCase(Locale.ROOT);
        // Tope del contrato: 32768
        if (s.startsWith("gpt-5") || s.contains("o3") || s.contains("o1") || s.contains("4o")) return 32_768;
        if (s.contains("mini") || s.contains("nano") || s.contains("o4-mini")) return 16_384;
        if (s.contains("codex") || s.contains("oss")) return 16_384;

        if (s.startsWith("gpt-4-32k")) return 8_192;
        if (s.startsWith("gpt-4-")) return 8_192;

        if (s.contains("3.5")) return 4_096;

        if (s.contains("chatgpt-4o-latest") || s.contains("gpt-5-chat-latest")) return 32_768;

        return 16_384;
    }

    // ------------------------------------------------------------------------------------------------
    // Snapshot curado (incluye TODOS los modelos relevantes de la lista del usuario)
    //   * Solo modalidades TEXT y STRUCTURED_JSON para cumplir OpenAPI
    //   * Estados marcados (ACTIVE/DEPRECATED)
    //   * Ventanas/outputs con valores seguros en los rangos del contrato
    // ------------------------------------------------------------------------------------------------
    private List<ModelSpec> curatedSnapshot() {
        ArrayList<ModelSpec> m = new ArrayList<>();

        // Frontier / GPT-5 family
        m.add(spec("gpt-5",                   ModelStatusEnum.ACTIVE,     200_000, 32_768));
        m.add(spec("gpt-5-mini",              ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("gpt-5-nano",              ModelStatusEnum.ACTIVE,     128_000,  8_192));
        m.add(spec("gpt-5-chat-latest",       ModelStatusEnum.ACTIVE,     200_000, 32_768)); // ChatGPT model (API no recomendada, pero activo)

        // gpt-4.1 family
        m.add(spec("gpt-4.1",                 ModelStatusEnum.ACTIVE,     128_000, 32_768));
        m.add(spec("gpt-4.1-mini",            ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("gpt-4.1-nano",            ModelStatusEnum.ACTIVE,     128_000,  8_192));

        // o-series (reasoning)
        m.add(spec("o3-pro",                  ModelStatusEnum.ACTIVE,     200_000, 32_768));
        m.add(spec("o3",                      ModelStatusEnum.ACTIVE,     128_000, 32_768));
        m.add(spec("o3-deep-research",        ModelStatusEnum.ACTIVE,     128_000, 32_768));
        m.add(spec("o3-mini",                 ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("o1-pro",                  ModelStatusEnum.ACTIVE,     200_000, 32_768));
        m.add(spec("o1",                      ModelStatusEnum.ACTIVE,     128_000, 32_768));
        m.add(spec("o1-mini",                 ModelStatusEnum.DEPRECATED, 128_000, 16_384));
        m.add(spec("o1-preview",              ModelStatusEnum.DEPRECATED, 128_000, 32_768));

        // o4-mini family
        m.add(spec("o4-mini",                  ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("o4-mini-deep-research",    ModelStatusEnum.ACTIVE,     128_000, 16_384));

        // gpt-4o
        m.add(spec("gpt-4o",                  ModelStatusEnum.ACTIVE,     128_000, 32_768));
        m.add(spec("gpt-4o-mini",             ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("gpt-4o-2024-05-13",       ModelStatusEnum.DEPRECATED, 128_000, 32_768));

        // Previews / search previews / specialized (seguimos marcando TEXT/STRUCTURED_JSON porque el contrato lo limita)
        m.add(spec("gpt-4o-mini-search-preview", ModelStatusEnum.DEPRECATED, 128_000, 16_384));
        m.add(spec("gpt-4o-search-preview",      ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("computer-use-preview",       ModelStatusEnum.ACTIVE,     128_000, 16_384));

        // Open-weight
        m.add(spec("gpt-oss-120b",            ModelStatusEnum.ACTIVE,     128_000, 16_384));
        m.add(spec("gpt-oss-20b",             ModelStatusEnum.ACTIVE,     128_000, 16_384));

        // ChatGPT “model” ids (no recomendados para API, pero presentes)
        m.add(spec("chatgpt-4o-latest",       ModelStatusEnum.DEPRECATED, 128_000, 32_768));

        // Older 4.x families
        m.add(spec("gpt-4",                   ModelStatusEnum.DEPRECATED,  8_192,   8_192));
        m.add(spec("gpt-4-turbo",             ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4-turbo-preview",     ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4.5-preview",         ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4-0125-preview",      ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4-1106-preview",      ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4-1106-vision-preview", ModelStatusEnum.DEPRECATED, 128_000, 32_768));
        m.add(spec("gpt-4-0613",              ModelStatusEnum.DEPRECATED,  8_192,   8_192));
        m.add(spec("gpt-4-0314",              ModelStatusEnum.DEPRECATED,  8_192,   8_192));
        m.add(spec("gpt-4-32k",               ModelStatusEnum.DEPRECATED, 32_768,   8_192));

        // 3.5 family y bases históricas (solo para catálogo/compatibilidad)
        m.add(spec("gpt-3.5-turbo",           ModelStatusEnum.DEPRECATED, 16_384,   4_096));
        m.add(spec("gpt-3.5-turbo-0125",      ModelStatusEnum.DEPRECATED, 16_384,   4_096));
        m.add(spec("gpt-3.5-turbo-1106",      ModelStatusEnum.DEPRECATED, 16_384,   4_096));
        m.add(spec("gpt-3.5-turbo-0613",      ModelStatusEnum.DEPRECATED, 16_384,   4_096));
        m.add(spec("gpt-3.5-0301",            ModelStatusEnum.DEPRECATED,  8_192,   4_096));
        m.add(spec("gpt-3.5-turbo-instruct",  ModelStatusEnum.DEPRECATED, 16_384,   4_096));
        m.add(spec("gpt-3.5-turbo-16k-0613",  ModelStatusEnum.DEPRECATED, 16_384,   4_096));

        // Code/Codex & base models (históricos)
        m.add(spec("codex-mini-latest",       ModelStatusEnum.ACTIVE,     64_000,   8_192));
        m.add(spec("davinci-002",             ModelStatusEnum.DEPRECATED,  8_192,   4_096));
        m.add(spec("babbage-002",             ModelStatusEnum.DEPRECATED,  8_192,   4_096));

        return m;
    }

    private ModelSpec spec(String id, ModelStatusEnum status, int contextWindow, int maxOutputTokens) {
        return ModelSpec.builder()
                .id(id)
                .provider(ModelProviderEnum.OPENAI)
                .modalities(DEFAULT_MODALITIES)
                .status(status)
                .contextWindow(contextWindow)
                .maxOutputTokens(maxOutputTokens)
                .build();
    }
}
