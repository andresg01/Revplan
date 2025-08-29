package com.revplan.llm.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationItem {
    private String modelId;
    @Builder.Default private List<ImageMatrixItem> matrix = new ArrayList<>();
}
