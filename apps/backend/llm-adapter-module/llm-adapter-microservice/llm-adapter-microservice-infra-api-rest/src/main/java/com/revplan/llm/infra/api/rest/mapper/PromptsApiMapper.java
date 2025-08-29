package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.enums.PromptVariableTypeEnum;
import com.revplan.llm.domain.model.Pagination;
import com.revplan.llm.domain.model.PromptTemplate;
import com.revplan.llm.domain.model.PromptTemplateListData;
import com.revplan.llm.domain.model.PromptVariable;
import com.revplan.llm.infra.api.dto.PromptTemplateDTO;
import com.revplan.llm.infra.api.dto.PromptTemplateDataDTO;
import com.revplan.llm.infra.api.dto.PromptTemplateListDataDTO;
import com.revplan.llm.infra.api.dto.PromptTemplateListDataPaginationDTO;
import com.revplan.llm.infra.api.dto.PromptVariableDTO;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = { PaginationApiMapper.class })
public interface PromptsApiMapper {

    // ====== Declaraciones abstractas para que MapStruct genere conversión de enums ======
    PromptVariableDTO.TypeEnum toDto(PromptVariableTypeEnum e);
    PromptVariableTypeEnum toModel(PromptVariableDTO.TypeEnum e);
    PromptTemplateListDataPaginationDTO toDto(Pagination p);

    // ====== Variables ======
    default PromptVariableDTO toDto(PromptVariable v) {
        if (v == null) return null;
        PromptVariableDTO dto = new PromptVariableDTO();
        dto.setName(v.getName());
        dto.setType(toDto(v.getType()));
        dto.setRequired(Boolean.TRUE.equals(v.getRequired()));
        dto.setDescription(v.getDescription());
        return dto;
    }

    default PromptVariable toModel(PromptVariableDTO dto) {
        if (dto == null) return null;
        return PromptVariable.builder()
                .name(dto.getName())
                .type(toModel(dto.getType()))
                .required(Boolean.TRUE.equals(dto.getRequired()))
                .description(dto.getDescription())
                .build();
    }

    // ====== Template ======
    default PromptTemplateDTO toDto(PromptTemplate t) {
        if (t == null) return null;
        PromptTemplateDTO dto = new PromptTemplateDTO();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setSystemPrompt(t.getSystemPrompt());
        dto.setUserTemplate(t.getUserTemplate());

        List<PromptVariableDTO> v = new ArrayList<>();
        if (t.getVariables() != null) for (PromptVariable pv : t.getVariables()) v.add(toDto(pv));
        dto.setVariables(v);

        dto.set_protected(Boolean.TRUE.equals(t.getProtectedFlag()));
        return dto;
    }

    default PromptTemplate toModel(PromptTemplateDTO dto) {
        if (dto == null) return null;
        List<PromptVariable> vars = new ArrayList<>();
        if (dto.getVariables() != null) for (PromptVariableDTO pv : dto.getVariables()) vars.add(toModel(pv));
        return PromptTemplate.builder()
                .id(dto.getId())
                .name(dto.getName())
                .systemPrompt(dto.getSystemPrompt())
                .userTemplate(dto.getUserTemplate())
                .variables(vars)
                .protectedFlag(Boolean.TRUE.equals(dto.get_protected()))
                .build();
    }

    // ====== Wrappers ======
    default PromptTemplateDataDTO toDataDto(PromptTemplate t) {
        if (t == null) return null;
        PromptTemplateDataDTO out = new PromptTemplateDataDTO();
        out.setData(toDto(t));
        return out;
    }

    default PromptTemplateListDataDTO toListDto(PromptTemplateListData list) {
        if (list == null) return null;
        PromptTemplateListDataDTO out = new PromptTemplateListDataDTO();

        List<PromptTemplateDTO> data = new ArrayList<>();
        if (list.getData() != null) for (PromptTemplate t : list.getData()) data.add(toDto(t));
        out.setData(data);

        out.setPagination(toDto(list.getPagination()));
        return out;
    }
}
