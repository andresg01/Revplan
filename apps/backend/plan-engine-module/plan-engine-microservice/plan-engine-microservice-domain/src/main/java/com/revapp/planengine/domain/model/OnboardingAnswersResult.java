package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.model.ProfileSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado tras enviar respuestas del onboarding:
 * - profile: snapshot derivado
 * - preview: (opcional) previsualización de score por plantilla
 * - recommendedTemplateId: (opcional) sugerencia de plantilla
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingAnswersResult {
    private ProfileSnapshot profile;
    private ScorePreviewData preview;
    private String recommendedTemplateId;
}
