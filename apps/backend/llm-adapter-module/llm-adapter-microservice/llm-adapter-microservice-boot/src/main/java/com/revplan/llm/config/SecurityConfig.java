package com.revplan.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Defines a BasicAuthWebSecurityConfiguration Security configuration
 * 
 * @author agarciab
 *
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http.headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
        .authorizeHttpRequests(requests -> requests
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/**"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/static/**"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/*"))
                .permitAll()
                .anyRequest()
                .authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}

}
