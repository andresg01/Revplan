package com.revapp.planengine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta: id de la pregunta + valor (String, Number, Boolean, List<String>, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingAnswer {
    private String id;
    private Object value;
}
