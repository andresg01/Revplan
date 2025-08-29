package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.enums.ResponseFormatEnum;
import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.GenerationParams;
import com.revplan.llm.domain.model.PlanAdjustments;
import com.revplan.llm.domain.model.PlanLLMOutput;
import com.revplan.llm.domain.model.PlanLLMOutputData;
import com.revplan.llm.infra.api.dto.GeneratePlanJsonRequestDTO;
import com.revplan.llm.infra.api.dto.GenerationParamsDTO;
import com.revplan.llm.infra.api.dto.PlanAdjustmentsDTO;
import com.revplan.llm.infra.api.dto.PlanLLMOutputDTO;
import com.revplan.llm.infra.api.dto.PlanLLMOutputDataDTO;
import com.revplan.llm.infra.api.dto.ResponseFormatDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PlansApiMapper {

    // ====== Declaraciones abstractas para enums ======
    ResponseFormatDTO toDto(ResponseFormatEnum e);
    ResponseFormatEnum toModel(ResponseFormatDTO e);

    // ====== GenerationParams ======
    default GenerationParamsDTO toDto(GenerationParams g) {
        if (g == null) return null;
        GenerationParamsDTO dto = new GenerationParamsDTO();
        dto.setModelId(g.getModelId());
        dto.setTemperature(g.getTemperature());
        dto.setTopP(g.getTopP());
        dto.setMaxOutputTokens(g.getMaxOutputTokens());
        dto.setSeed(g.getSeed());
        dto.setPresencePenalty(g.getPresencePenalty());
        dto.setFrequencyPenalty(g.getFrequencyPenalty());
        return dto;
    }

    default GenerationParams toModel(GenerationParamsDTO dto) {
        if (dto == null) return null;
        return GenerationParams.builder()
                .modelId(dto.getModelId())
                .temperature(dto.getTemperature())
                .topP(dto.getTopP())
                .maxOutputTokens(dto.getMaxOutputTokens())
                .seed(dto.getSeed())
                .presencePenalty(dto.getPresencePenalty())
                .frequencyPenalty(dto.getFrequencyPenalty())
                .build();
    }

    // ====== PlanAdjustments ======
    default PlanAdjustmentsDTO toDto(PlanAdjustments a) {
        if (a == null) return null;
        PlanAdjustmentsDTO dto = new PlanAdjustmentsDTO();
        dto.setSavingPct(a.getSavingPct());
        dto.setEmergencyMonths(a.getEmergencyMonths());
        dto.setEnvelopes(a.getEnvelopes() == null ? new HashMap<>() : new HashMap<>(a.getEnvelopes()));
        return dto;
    }

    default PlanAdjustments toModel(PlanAdjustmentsDTO dto) {
        if (dto == null) return null;
        return PlanAdjustments.builder()
                .savingPct(dto.getSavingPct())
                .emergencyMonths(dto.getEmergencyMonths())
                .envelopes(dto.getEnvelopes() == null ? new HashMap<>() : new HashMap<>(dto.getEnvelopes()))
                .build();
    }

    // ====== PlanLLMOutput ======
    default PlanLLMOutputDTO toDto(PlanLLMOutput o) {
        if (o == null) return null;
        PlanLLMOutputDTO dto = new PlanLLMOutputDTO();
        dto.setTemplateId(o.getTemplateId());
        dto.setAdjustments(toDto(o.getAdjustments()));
        dto.setRationale(o.getRationale());
        List<String> alerts = new ArrayList<>();
        if (o.getAlerts() != null) alerts.addAll(o.getAlerts());
        dto.setAlerts(alerts);
        return dto;
    }

    default PlanLLMOutput toModel(PlanLLMOutputDTO dto) {
        if (dto == null) return null;
        List<String> alerts = new ArrayList<>();
        if (dto.getAlerts() != null) alerts.addAll(dto.getAlerts());
        return PlanLLMOutput.builder()
                .templateId(dto.getTemplateId())
                .adjustments(toModel(dto.getAdjustments()))
                .rationale(dto.getRationale())
                .alerts(alerts)
                .build();
    }

    // ====== Wrappers ======
    default PlanLLMOutputDataDTO toDataDto(PlanLLMOutput output) {
        if (output == null) return null;
        PlanLLMOutputDataDTO d = new PlanLLMOutputDataDTO();
        d.setData(toDto(output));
        return d;
    }

    default PlanLLMOutputData toModel(PlanLLMOutputDataDTO dto) {
        if (dto == null) return null;
        return PlanLLMOutputData.builder()
                .data(toModel(dto.getData()))
                .build();
    }

    // ====== GeneratePlanJsonRequest ======
    default GeneratePlanJsonRequest toModel(GeneratePlanJsonRequestDTO dto) {
        if (dto == null) return null;
        return GeneratePlanJsonRequest.builder()
                .templateId(dto.getTemplateId())
                .variables(dto.getVariables() == null ? new HashMap<>() : new HashMap<>(dto.getVariables()))
                .responseFormat(toModel(dto.getResponseFormat()))
                .schema(dto.getSchema())
                .generation(toModel(dto.getGeneration()))
                .build();
    }

    default GeneratePlanJsonRequestDTO toDto(GeneratePlanJsonRequest m) {
        if (m == null) return null;
        GeneratePlanJsonRequestDTO dto = new GeneratePlanJsonRequestDTO();
        dto.setTemplateId(m.getTemplateId());
        dto.setVariables(m.getVariables() == null ? new HashMap<>() : new HashMap<>(m.getVariables()));
        dto.setResponseFormat(toDto(m.getResponseFormat()));
        dto.setSchema(m.getSchema());
        dto.setGeneration(toDto(m.getGeneration()));
        return dto;
    }
}
