package com.revapp.planengine.infra.persistence.jpa.converters;

import com.revapp.planengine.domain.enums.OnboardingQuestionTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OnboardingQuestionTypeConverter implements AttributeConverter<OnboardingQuestionTypeEnum, String> {

    @Override
    public String convertToDatabaseColumn(OnboardingQuestionTypeEnum attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case INTEGER       -> "integer";
            case NUMBER        -> "number";
            case STRING        -> "string";
            case BOOLEAN       -> "boolean";
            case SINGLE_SELECT -> "single_select";
            case MULTI_SELECT  -> "multi_select";
            case CURRENCY      -> "number";
            case DATE          -> "string";
        };
    }

    @Override
    public OnboardingQuestionTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "integer"       -> OnboardingQuestionTypeEnum.INTEGER;
            case "number"        -> OnboardingQuestionTypeEnum.NUMBER;   // también representa CURRENCY en BBDD
            case "string"        -> OnboardingQuestionTypeEnum.STRING;   // también representa DATE en BBDD
            case "boolean"       -> OnboardingQuestionTypeEnum.BOOLEAN;
            case "single_select" -> OnboardingQuestionTypeEnum.SINGLE_SELECT;
            case "multi_select"  -> OnboardingQuestionTypeEnum.MULTI_SELECT;
            default -> throw new IllegalArgumentException("Unknown qtype: " + dbData);
        };
    }
}
