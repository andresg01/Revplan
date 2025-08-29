package com.revplan.llm.domain.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Pagination {
    private Integer offset;
    private Integer limit;
    private Integer totalElements;
    private Integer totalPages;
    private Integer pageNumber;
}
