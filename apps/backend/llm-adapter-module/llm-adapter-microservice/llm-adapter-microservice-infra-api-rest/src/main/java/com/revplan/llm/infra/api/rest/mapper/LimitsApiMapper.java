package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.model.Limits;
import com.revplan.llm.infra.api.dto.GetLimits200ResponseDTO;

@Mapper(componentModel = "spring")
public interface LimitsApiMapper {

    default GetLimits200ResponseDTO toDto(Limits l) {
        if (l == null) return null;
        GetLimits200ResponseDTO dto = new GetLimits200ResponseDTO();
        dto.setLimitPerMinute(l.getLimitPerMinute());
        dto.setEstimatedUsageMinute(l.getEstimatedUsageMinute());
        dto.setBurstAllowed(l.getBurstAllowed());
        return dto;
    }

    default Limits toModel(GetLimits200ResponseDTO dto) {
        if (dto == null) return null;
        return Limits.builder()
                .limitPerMinute(dto.getLimitPerMinute())
                .estimatedUsageMinute(dto.getEstimatedUsageMinute())
                .burstAllowed(dto.getBurstAllowed())
                .build();
    }
}
