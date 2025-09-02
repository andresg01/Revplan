package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.OnboardingQuestionSectionEnum;
import com.revapp.planengine.domain.enums.OnboardingQuestionTypeEnum;
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
public class OnboardingQuestion {
    private String id;
    private String text;
    private OnboardingQuestionTypeEnum type;

    @Builder.Default
    private Boolean required = Boolean.TRUE;

    private OnboardingQuestionSectionEnum section;
    private OnboardingQuestionConstraints constraints;

    @Builder.Default
    private List<String> options = new ArrayList<>();
}
