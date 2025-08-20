package com.revapp.planengine.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.HttpClient;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(OpenAIProps.class)
public class OpenAIConfig {

    @Bean("openAiWebClient")
    WebClient openAiWebClient(OpenAIProps props) {
        int readWriteMs = props.getTimeoutMs();
        int connectMs   = Math.min(10_000, readWriteMs); // 10s o menos si el total es menor

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(readWriteMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readWriteMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(readWriteMs, TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(props.getMaxInMemoryBytes()))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(props.getBaseUrl())
                .defaultHeaders(h -> {
                    h.setBearerAuth(props.getApiKey());
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    h.add("OpenAI-Beta", "responses-structured-outputs=v1");
                })
                .exchangeStrategies(strategies)
                .build();
    }
}

@Data
@Validated
@ConfigurationProperties(prefix = "openai")
class OpenAIProps {
    /** Se inyecta desde env var OPENAI_API_KEY o application.yml */
    @NotBlank
    private String apiKey;

    /** Se puede sobreescribir con OPENAI_BASE_URL */
    @NotBlank
    private String baseUrl = "https://api.openai.com/v1";

    /** Timeout total de lectura/respuesta (OPENAI_TIMEOUT_MS) */
    @Min(1000)
    private Integer timeoutMs = 90_000; // 90s por defecto

    /** Límite de memoria para deserializar respuestas (OPENAI_MAX_IN_MEMORY_BYTES) */
    @Min(1024)
    private Integer maxInMemoryBytes = 2 * 1024 * 1024; // 2MB
}
