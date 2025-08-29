package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.model.PlanLLMOutputSchemaDoc;
import com.revplan.llm.domain.model.ValidateJsonError;
import com.revplan.llm.domain.model.ValidateJsonResult;
import com.revplan.llm.domain.model.ValidationTarget;
import com.revplan.llm.infra.api.dto.PlanLLMOutputSchemaDocDTO;
import com.revplan.llm.infra.api.dto.ValidateJsonAgainstSchema200ResponseDTO;
import com.revplan.llm.infra.api.dto.ValidateJsonAgainstSchema200ResponseErrorsInnerDTO;
import com.revplan.llm.infra.api.dto.ValidationTargetDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SchemasApiMapper {

    // ValidationTarget
    default ValidationTarget toModel(ValidationTargetDTO dto) {
        if (dto == null) return null;
        return ValidationTarget.builder()
                .schema(dto.getSchema())
                .json(dto.getJson())
                .build();
    }

    default ValidationTargetDTO toDto(ValidationTarget m) {
        if (m == null) return null;
        ValidationTargetDTO dto = new ValidationTargetDTO();
        dto.setSchema(m.getSchema());
        dto.setJson(m.getJson());
        return dto;
    }

    // Validate JSON result
    default ValidateJsonAgainstSchema200ResponseDTO toDto(ValidateJsonResult r) {
        if (r == null) return null;
        ValidateJsonAgainstSchema200ResponseDTO dto = new ValidateJsonAgainstSchema200ResponseDTO();
        dto.setValid(Boolean.TRUE.equals(r.getValid()));
        List<ValidateJsonAgainstSchema200ResponseErrorsInnerDTO> list = new ArrayList<>();
        if (r.getErrors() != null) {
            for (ValidateJsonError e : r.getErrors()) {
                ValidateJsonAgainstSchema200ResponseErrorsInnerDTO ee = new ValidateJsonAgainstSchema200ResponseErrorsInnerDTO();
                ee.setPath(e.getPath());
                ee.setMessage(e.getMessage());
                list.add(ee);
            }
        }
        dto.setErrors(list);
        return dto;
    }

    // PlanLLMOutputSchemaDoc
    default PlanLLMOutputSchemaDocDTO toDto(PlanLLMOutputSchemaDoc s) {
        if (s == null) return null;
        PlanLLMOutputSchemaDocDTO dto = new PlanLLMOutputSchemaDocDTO();
        dto.setTitle(s.getTitle());
        dto.setType(s.getType());
        if (s.getAdditionalProperties() != null) {
            s.getAdditionalProperties().forEach(dto::putAdditionalProperty);
        }
        return dto;
    }

    default PlanLLMOutputSchemaDoc toModel(PlanLLMOutputSchemaDocDTO dto) {
        if (dto == null) return null;
        PlanLLMOutputSchemaDoc s = new PlanLLMOutputSchemaDoc();
        s.setTitle(dto.getTitle());
        s.setType(dto.getType());
        s.setAdditionalProperties(dto.getAdditionalProperties() == null
                ? new HashMap<>()
                : new HashMap<>(dto.getAdditionalProperties()));
        return s;
    }
}
