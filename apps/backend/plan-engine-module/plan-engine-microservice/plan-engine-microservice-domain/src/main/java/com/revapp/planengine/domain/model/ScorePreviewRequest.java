package com.revapp.planengine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ScorePreviewRequest {
    private String nameFilter; // opcional (filtrado por plantilla)
}
