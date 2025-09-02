package com.revapp.planengine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitOnboardingAnswersRequest {
    private UUID userId;

    @Builder.Default
    private List<OnboardingAnswer> answers = new ArrayList<>();

    @Builder.Default
    private Boolean previewScores = Boolean.TRUE;

    @Builder.Default
    private Boolean recommendTemplate = Boolean.TRUE;
}
