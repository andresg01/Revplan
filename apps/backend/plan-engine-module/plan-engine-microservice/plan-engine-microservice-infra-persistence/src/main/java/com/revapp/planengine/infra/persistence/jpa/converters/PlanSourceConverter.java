package com.revapp.planengine.infra.persistence.jpa.converters;

import com.revapp.planengine.domain.enums.PlanSourceEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanSourceConverter implements AttributeConverter<PlanSourceEnum, String> {

    @Override
    public String convertToDatabaseColumn(PlanSourceEnum attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case RULES -> "rules";
            case RULES_PLUS_GPT -> "rules+gpt"; // BD usa '+'
        };
    }

    @Override
    public PlanSourceEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "rules"    -> PlanSourceEnum.RULES;
            case "rules+gpt"-> PlanSourceEnum.RULES_PLUS_GPT;
            default -> throw new IllegalArgumentException("Unknown PlanSource db value: " + dbData);
        };
    }
}
