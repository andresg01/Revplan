package com.revapp.planengine.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlanStatusEnum {
    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PlanStatusEnum from(String value) {
        if (value == null) return null;
        return PlanStatusEnum.valueOf(value.trim().toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
