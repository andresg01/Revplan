package com.revapp.planengine.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(LlmAdapterProps.class)
public class LlmAdapterConfig {

    @Bean("llmAdapterWebClient")
    WebClient llmAdapterWebClient(LlmAdapterProps props) {
        int rw = props.getTimeoutMs();
        int connect = Math.min(10_000, rw);

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connect)
                .responseTimeout(Duration.ofMillis(rw))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(rw, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(rw, TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(props.getMaxInMemoryBytes()))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(http))
                .baseUrl(props.getBaseUrl())
                .defaultHeaders(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                    // BASIC AUTH (preemptive)
                    if (props.getUsername() != null && !props.getUsername().isBlank()) {
                        h.setBasicAuth(props.getUsername(), props.getPassword() == null ? "" : props.getPassword());
                    }
                })
                .exchangeStrategies(strategies)
                .build();
    }
}

@Data
@Validated
@ConfigurationProperties(prefix = "llm.adapter")
class LlmAdapterProps {
    @NotBlank
    private String baseUrl;

    @Min(1000)
    private Integer timeoutMs = 90_000;

    @Min(1024)
    private Integer maxInMemoryBytes = 2 * 1024 * 1024;

    private String username;

    private String password;
}
