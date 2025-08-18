package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.api.dto.PaginationDTO;

public final class PaginationApiMapper {
    private PaginationApiMapper() {}

    public static PaginationDTO of(PageResult<?> page) {
        if (page == null) return null;
        PaginationDTO dto = new PaginationDTO();
        dto.setOffset(page.offset());
        dto.setLimit(page.limit());
        dto.setTotalElements((int)Math.min(Integer.MAX_VALUE, page.total()));
        // pageNumber/totalPages pueden calcularse si quieres:
        if (page.limit() > 0) {
            int pageNumber = page.offset() / page.limit(); // 0-based
            int totalPages = (int) Math.ceil((double) page.total() / page.limit());
            dto.setPageNumber(pageNumber);
            dto.setTotalPages(totalPages);
        }
        return dto;
    }
}
