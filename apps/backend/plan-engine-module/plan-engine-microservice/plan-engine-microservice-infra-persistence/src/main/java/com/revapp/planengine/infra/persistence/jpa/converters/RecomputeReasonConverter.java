package com.revapp.planengine.infra.persistence.jpa.converters;

import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RecomputeReasonConverter implements AttributeConverter<RecomputeReasonEnum, String> {

    @Override
    public String convertToDatabaseColumn(RecomputeReasonEnum attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case BALANCE_CHANGE  -> "balance_update";
            case SPEND_DRIFT     -> "overspend";
            case PERIODIC_REVIEW -> "sync";
            case MANUAL          -> "manual";
        };
    }

    @Override
    public RecomputeReasonEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "balance_update" -> RecomputeReasonEnum.BALANCE_CHANGE;
            case "overspend"      -> RecomputeReasonEnum.SPEND_DRIFT;
            case "sync"           -> RecomputeReasonEnum.PERIODIC_REVIEW;
            case "manual"         -> RecomputeReasonEnum.MANUAL;
            default -> throw new IllegalArgumentException("Unknown RecomputeReason db value: " + dbData);
        };
    }
}
