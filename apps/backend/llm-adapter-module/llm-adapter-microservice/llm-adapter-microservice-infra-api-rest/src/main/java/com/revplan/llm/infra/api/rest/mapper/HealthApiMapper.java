package com.revplan.llm.infra.api.rest.mapper;

import com.revplan.llm.domain.model.HealthLLM;
import com.revplan.llm.infra.api.dto.HealthLLM200ResponseDTO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class HealthApiMapper {

    public HealthLLM200ResponseDTO toDto(HealthLLM h) {
        if (h == null) return null;

        HealthLLM200ResponseDTO dto = new HealthLLM200ResponseDTO();
        dto.setProvider(h.getProvider());
        dto.setReachable(h.getReachable());
        dto.setModelLatencyMs(h.getModelLatencyMs());
        // Domain usa LocalDateTime; DTO usa OffsetDateTime
        dto.setTimestamp(h.getTimestamp() == null ? null : h.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDateTime());
        return dto;
    }
}
