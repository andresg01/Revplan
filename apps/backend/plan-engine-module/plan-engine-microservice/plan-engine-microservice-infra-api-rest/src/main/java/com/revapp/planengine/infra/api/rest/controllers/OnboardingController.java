package com.revapp.planengine.infra.api.rest.controllers;

import com.revapp.planengine.infra.api.dto.*;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class OnboardingController implements OnboardingApi {

    @Override
    public ResponseEntity<OnboardingQuestionsDataDTO> getOnboardingQuestions(UUID xRequestID, String acceptLanguage) {
        return OnboardingApi.super.getOnboardingQuestions(xRequestID, acceptLanguage);
    }

    @Override
    public ResponseEntity<OnboardingAnswersDataDTO> submitOnboardingAnswers(UUID xRequestID, SubmitOnboardingAnswersRequestDTO submitOnboardingAnswersRequestDTO, String acceptLanguage) {
        return OnboardingApi.super.submitOnboardingAnswers(xRequestID, submitOnboardingAnswersRequestDTO, acceptLanguage);
    }

    @Override
    public ResponseEntity<PlanDataDTO> completeOnboardingAndGeneratePlan(UUID xRequestID, CompleteOnboardingAndGeneratePlanRequestDTO completeOnboardingAndGeneratePlanRequestDTO, String acceptLanguage) {
        return OnboardingApi.super.completeOnboardingAndGeneratePlan(xRequestID, completeOnboardingAndGeneratePlanRequestDTO, acceptLanguage);
    }
}
