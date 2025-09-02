package com.revapp.planengine.domain.enums;

/**
 * Modo de generación durante el onboarding.
 * Equivale a los valores del DTO (rules | rules_plus_gpt)
 * y es coherente con PlanSourceEnum a nivel semántico.
 */
public enum OnboardingModeEnum {
    RULES,
    RULES_PLUS_GPT
}
