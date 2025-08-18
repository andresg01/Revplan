package com.revapp.planengine.domain.model;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScorePreviewData {
    @Builder.Default
    private List<ScorePreviewItem> data = new ArrayList<>();
}
