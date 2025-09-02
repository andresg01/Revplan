package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.OnboardingModeEnum;
import com.revapp.planengine.domain.model.AiOptions;
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
public class CompleteOnboardingAndGeneratePlanRequest {
    private UUID userId;

    @Builder.Default
    private List<OnboardingAnswer> answers = new ArrayList<>();

    @Builder.Default
    private OnboardingModeEnum mode = OnboardingModeEnum.RULES_PLUS_GPT;

    private String forceTemplateId;

    @Builder.Default
    private Boolean explain = Boolean.TRUE;

    private AiOptions aiOptions;
}
