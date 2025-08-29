package com.revplan.llm.domain.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateJsonResult {
    private Boolean valid;
    @Builder.Default private List<ValidateJsonError> errors = new ArrayList<>();
}
