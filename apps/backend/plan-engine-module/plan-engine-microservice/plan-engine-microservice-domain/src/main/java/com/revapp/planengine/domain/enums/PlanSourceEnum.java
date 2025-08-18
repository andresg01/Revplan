package com.revapp.planengine.domain.enums;

public enum PlanSourceEnum {
    RULES("rules"),
    RULES_PLUS_GPT("rules_plus_gpt");

    private final String apiValue;
    PlanSourceEnum(String apiValue) { this.apiValue = apiValue; }
    public String getApiValue() { return apiValue; }

    public static PlanSourceEnum fromApi(String v) {
        if (v == null) return null;
        switch (v) {
            case "rules": return RULES;
            case "rules_plus_gpt": return RULES_PLUS_GPT;
            default: throw new IllegalArgumentException("Unknown mode: " + v);
        }
    }
}
