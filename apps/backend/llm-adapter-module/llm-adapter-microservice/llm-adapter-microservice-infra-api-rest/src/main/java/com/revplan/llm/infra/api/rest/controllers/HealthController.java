package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.domain.service.HealthService;
import com.revplan.llm.infra.api.dto.HealthLLM200ResponseDTO;
import com.revplan.llm.infra.api.rest.mapper.HealthApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController implements HealthApi {

    private final HealthService healthService;
    private final HealthApiMapper mapper;

    @Override
    public ResponseEntity<HealthLLM200ResponseDTO> healthLLM() {
        var h = healthService.check();
        return ResponseEntity.ok(mapper.toDto(h));
    }
}
