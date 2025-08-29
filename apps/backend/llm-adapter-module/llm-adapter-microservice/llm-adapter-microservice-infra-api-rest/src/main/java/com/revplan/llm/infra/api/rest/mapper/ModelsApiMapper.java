package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.enums.ModelModalityEnum;
import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.enums.ModelStatusEnum;
import com.revplan.llm.domain.model.ModelSpec;
import com.revplan.llm.domain.model.ModelsData;
import com.revplan.llm.infra.api.dto.GetModels200ResponseDTO;
import com.revplan.llm.infra.api.dto.ModelDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ModelsApiMapper {

    // ====== Declaraciones abstractas para enums (MapStruct las genera por nombre) ======
    ModelDTO.ProviderEnum toDto(ModelProviderEnum e);
    ModelProviderEnum toModel(ModelDTO.ProviderEnum e);

    ModelDTO.ModalitiesEnum toDto(ModelModalityEnum e);
    ModelModalityEnum toModel(ModelDTO.ModalitiesEnum e);

    ModelDTO.StatusEnum toDto(ModelStatusEnum e);
    ModelStatusEnum toModel(ModelDTO.StatusEnum e);

    // ====== ModelSpec -> DTO ======
    default ModelDTO toDto(ModelSpec m) {
        if (m == null) return null;
        ModelDTO dto = new ModelDTO();
        dto.setId(m.getId());
        dto.setProvider(toDto(m.getProvider()));
        dto.setContextWindow(m.getContextWindow());
        dto.setMaxOutputTokens(m.getMaxOutputTokens());

        // ModelDTO.setModalities espera un Set, no una List
        Set<ModelDTO.ModalitiesEnum> mods = new LinkedHashSet<>();
        if (m.getModalities() != null) {
            for (ModelModalityEnum mm : m.getModalities()) {
                mods.add(toDto(mm));
            }
        }
        dto.setModalities(mods);

        dto.setStatus(toDto(m.getStatus()));
        return dto;
    }

    // ====== DTO -> ModelSpec ======
    default ModelSpec toModel(ModelDTO dto) {
        if (dto == null) return null;

        // En el dominio seguimos usando List; el DTO trae Set
        List<ModelModalityEnum> mods = new ArrayList<>();
        if (dto.getModalities() != null) {
            for (ModelDTO.ModalitiesEnum mm : dto.getModalities()) {
                mods.add(toModel(mm));
            }
        }

        return ModelSpec.builder()
                .id(dto.getId())
                .provider(toModel(dto.getProvider()))
                .contextWindow(dto.getContextWindow())
                .maxOutputTokens(dto.getMaxOutputTokens())
                .modalities(mods)
                .status(toModel(dto.getStatus()))
                .build();
    }

    // ====== Wrapper ======
    default GetModels200ResponseDTO toListDto(ModelsData data) {
        if (data == null) return null;
        GetModels200ResponseDTO out = new GetModels200ResponseDTO();
        List<ModelDTO> list = new ArrayList<>();
        if (data.getData() != null) {
            for (ModelSpec m : data.getData()) {
                list.add(toDto(m));
            }
        }
        out.setData(list);
        return out;
    }
}
