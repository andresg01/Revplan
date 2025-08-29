package com.revplan.llm.infra.api.rest.mapper;

import org.mapstruct.Mapper;

import com.revplan.llm.domain.model.Pagination;
import com.revplan.llm.infra.api.dto.PromptTemplateListDataPaginationDTO;

@Mapper(componentModel = "spring")
public interface PaginationApiMapper {

    default PromptTemplateListDataPaginationDTO toDto(Pagination p) {
        if (p == null) return null;
        PromptTemplateListDataPaginationDTO dto = new PromptTemplateListDataPaginationDTO();
        dto.setOffset(p.getOffset());
        dto.setLimit(p.getLimit());
        dto.setTotalElements(p.getTotalElements());
        dto.setTotalPages(p.getTotalPages());
        dto.setPageNumber(p.getPageNumber());
        return dto;
    }

    default Pagination toModel(PromptTemplateListDataPaginationDTO dto) {
        if (dto == null) return null;
        return Pagination.builder()
                .offset(dto.getOffset())
                .limit(dto.getLimit())
                .totalElements(dto.getTotalElements())
                .totalPages(dto.getTotalPages())
                .pageNumber(dto.getPageNumber())
                .build();
    }
}
