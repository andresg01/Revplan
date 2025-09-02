package com.revapp.planengine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingQuestionsPayload {
    private String version;

    @Builder.Default
    private List<OnboardingQuestion> questions = new ArrayList<>();
}
