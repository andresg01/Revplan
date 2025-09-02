package com.revapp.planengine.domain.enums;

public enum OnboardingQuestionTypeEnum {
    INTEGER,
    NUMBER,
    STRING,
    BOOLEAN,
    SINGLE_SELECT,   // API: choice
    MULTI_SELECT,    // API: multi_choice
    CURRENCY,        // API: currency (semánticamente número con formato)
    DATE             // API: date (semánticamente string con formato)
}
