package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.enums.ModelModalityEnum;
import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.enums.ModelStatusEnum;
import com.revplan.llm.domain.enums.PromptVariableTypeEnum;
import com.revplan.llm.domain.enums.ResponseFormatEnum;

import com.revplan.llm.infra.api.dto.ModelDTO;
import com.revplan.llm.infra.api.dto.PromptVariableDTO;
import com.revplan.llm.infra.api.dto.ResponseFormatDTO;

@Mapper(componentModel = "spring")
public interface EnumApiMapper {

    // -------- ResponseFormat --------
    default ResponseFormatEnum toModel(ResponseFormatDTO dto) {
        if (dto == null) return null;
        switch (dto) {
            case JSON_SCHEMA: return ResponseFormatEnum.JSON_SCHEMA;
        }
        throw new IllegalArgumentException("Unknown ResponseFormatDTO: " + dto);
    }

    default ResponseFormatDTO toDto(ResponseFormatEnum e) {
        if (e == null) return null;
        switch (e) {
            case JSON_SCHEMA: return ResponseFormatDTO.JSON_SCHEMA;
        }
        throw new IllegalArgumentException("Unknown ResponseFormatEnum: " + e);
    }

    // -------- Model provider --------
    default ModelProviderEnum toModel(ModelDTO.ProviderEnum dto) {
        if (dto == null) return null;
        switch (dto) {
            case OPENAI: return ModelProviderEnum.OPENAI;
            case AZURE_OPENAI: return ModelProviderEnum.AZURE_OPENAI;
        }
        throw new IllegalArgumentException("Unknown ProviderEnum: " + dto);
    }

    default ModelDTO.ProviderEnum toDto(ModelProviderEnum e) {
        if (e == null) return null;
        switch (e) {
            case OPENAI: return ModelDTO.ProviderEnum.OPENAI;
            case AZURE_OPENAI: return ModelDTO.ProviderEnum.AZURE_OPENAI;
        }
        throw new IllegalArgumentException("Unknown ModelProviderEnum: " + e);
    }

    // -------- Model modality --------
    default ModelModalityEnum toModel(ModelDTO.ModalitiesEnum dto) {
        if (dto == null) return null;
        switch (dto) {
            case TEXT: return ModelModalityEnum.TEXT;
            case STRUCTURED_JSON: return ModelModalityEnum.STRUCTURED_JSON;
        }
        throw new IllegalArgumentException("Unknown ModalitiesEnum: " + dto);
    }

    default ModelDTO.ModalitiesEnum toDto(ModelModalityEnum e) {
        if (e == null) return null;
        switch (e) {
            case TEXT: return ModelDTO.ModalitiesEnum.TEXT;
            case STRUCTURED_JSON: return ModelDTO.ModalitiesEnum.STRUCTURED_JSON;
        }
        throw new IllegalArgumentException("Unknown ModelModalityEnum: " + e);
    }

    // -------- Model status --------
    default ModelStatusEnum toModel(ModelDTO.StatusEnum dto) {
        if (dto == null) return null;
        switch (dto) {
            case ACTIVE: return ModelStatusEnum.ACTIVE;
            case DEPRECATED: return ModelStatusEnum.DEPRECATED;
            case UNAVAILABLE: return ModelStatusEnum.UNAVAILABLE;
        }
        throw new IllegalArgumentException("Unknown StatusEnum: " + dto);
    }

    default ModelDTO.StatusEnum toDto(ModelStatusEnum e) {
        if (e == null) return null;
        switch (e) {
            case ACTIVE: return ModelDTO.StatusEnum.ACTIVE;
            case DEPRECATED: return ModelDTO.StatusEnum.DEPRECATED;
            case UNAVAILABLE: return ModelDTO.StatusEnum.UNAVAILABLE;
        }
        throw new IllegalArgumentException("Unknown ModelStatusEnum: " + e);
    }

    // -------- Prompt variable type --------
    default PromptVariableTypeEnum toModel(PromptVariableDTO.TypeEnum dto) {
        if (dto == null) return null;
        switch (dto) {
            case STRING: return PromptVariableTypeEnum.STRING;
            case NUMBER: return PromptVariableTypeEnum.NUMBER;
            case BOOLEAN: return PromptVariableTypeEnum.BOOLEAN;
            case OBJECT: return PromptVariableTypeEnum.OBJECT;
            case ARRAY: return PromptVariableTypeEnum.ARRAY;
        }
        throw new IllegalArgumentException("Unknown PromptVariableDTO.TypeEnum: " + dto);
    }

    default PromptVariableDTO.TypeEnum toDto(PromptVariableTypeEnum e) {
        if (e == null) return null;
        switch (e) {
            case STRING: return PromptVariableDTO.TypeEnum.STRING;
            case NUMBER: return PromptVariableDTO.TypeEnum.NUMBER;
            case BOOLEAN: return PromptVariableDTO.TypeEnum.BOOLEAN;
            case OBJECT: return PromptVariableDTO.TypeEnum.OBJECT;
            case ARRAY: return PromptVariableDTO.TypeEnum.ARRAY;
        }
        throw new IllegalArgumentException("Unknown PromptVariableTypeEnum: " + e);
    }
}
