package com.revplan.llm.domain.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateJsonError {
    private String path;
    private String message;
}
