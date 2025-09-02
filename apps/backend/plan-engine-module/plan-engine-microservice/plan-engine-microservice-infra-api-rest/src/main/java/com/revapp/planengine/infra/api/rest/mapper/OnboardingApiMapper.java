package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.enums.*;
import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.infra.api.dto.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapper DTO <-> Domain para el flujo de Onboarding usando MapStruct.
 * Este mapper reemplaza la implementación manual por una declarativa,
 * mejorando la seguridad, legibilidad y mantenimiento del código.
 */
@Mapper(componentModel = "spring",
        // Estrategia para que los enums coincidan ignorando mayúsculas/minúsculas
        // Esto simplifica el mapeo de PERFIL -> PERFIL("perfil")
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        unmappedTargetPolicy = ReportingPolicy.IGNORE) // Ignora advertencias si no todos los campos se mapean
public interface OnboardingApiMapper {

    // =====================================================================
    // DTO -> Domain Model
    // =====================================================================

    SubmitOnboardingAnswersRequest toModel(SubmitOnboardingAnswersRequestDTO dto);

    CompleteOnboardingAndGeneratePlanRequest toModel(CompleteOnboardingAndGeneratePlanRequestDTO dto);

    OnboardingAnswer toModel(OnboardingAnswerDTO dto);

    OnboardingQuestionsPayload toModel(OnboardingQuestionsPayloadDTO dto);

    OnboardingQuestion toModel(OnboardingQuestionDTO dto);

    OnboardingQuestionConstraints toModel(OnboardingQuestionConstraintsDTO dto);

    AiOptions toModel(AiOptionsDTO dto);

    // =====================================================================
    // Domain Model -> DTO
    // =====================================================================

    /**
     * Mapea el payload de preguntas a su DTO contenedor (Wrapper).
     * MapStruct usará el metodo 'toDto(OnboardingQuestionsPayload)' para convertir el payload.
     */
    @Mapping(target = "data", source = "payload")
    OnboardingQuestionsDataDTO toQuestionsDataDto(OnboardingQuestionsPayload payload);

    OnboardingQuestionsPayloadDTO toDto(OnboardingQuestionsPayload payload);

    OnboardingQuestionDTO toDto(OnboardingQuestion question);

    OnboardingQuestionConstraintsDTO toDto(OnboardingQuestionConstraints constraints);

    /**
     * Mapea el resultado de las respuestas a su DTO contenedor (Wrapper).
     * MapStruct usará el metodo 'toDto(OnboardingAnswersResult)' para convertir el resultado.
     */
    @Mapping(target = "data", source = "result")
    OnboardingAnswersDataDTO toAnswersDataDto(OnboardingAnswersResult result);

    OnboardingAnswersResultDTO toDto(OnboardingAnswersResult result);

    ProfileSnapshotDTO toDto(ProfileSnapshot profileSnapshot);

    @Mapping(target = "data", source = "data")
    ScorePreviewDataDTO toDto(ScorePreviewData scorePreviewData);

    /**
     * Mapea un item de la preview. Se usa un metodo custom ('mapFactors')
     * para la conversión especial del mapa 'factors'.
     */
    @Mapping(target = "factors", source = "factors", qualifiedByName = "mapFactors")
    ScorePreviewItemDTO toDto(ScorePreviewItem item);

    // =====================================================================
    // Enum Mappers
    // =====================================================================

    /**
     * Mapeo explícito para OnboardingQuestionTypeEnum donde los nombres no coinciden.
     * Ejemplo: CHOICE (DTO) <-> SINGLE_SELECT (Domain).
     */
    @ValueMappings({
            @ValueMapping(source = "CHOICE", target = "SINGLE_SELECT"),
            @ValueMapping(source = "MULTI_CHOICE", target = "MULTI_SELECT")
    })
    OnboardingQuestionTypeEnum toModel(OnboardingQuestionTypeDTO dto);

    /**
     * Mapeo inverso. @InheritInverseConfiguration reutiliza los mapeos de arriba.
     * Se añade una regla específica para mapear INTEGER (Domain) -> NUMBER (DTO).
     */
    @InheritInverseConfiguration
    @ValueMapping(source = "INTEGER", target = "NUMBER")
    OnboardingQuestionTypeDTO toDto(OnboardingQuestionTypeEnum model);

    // Para los siguientes enums, MapStruct puede inferir la relación por el nombre
    // (ignorando mayúsculas/minúsculas), pero definirlos es más robusto.
    OnboardingQuestionSectionEnum toModel(OnboardingQuestionDTO.SectionEnum dto);
    OnboardingQuestionDTO.SectionEnum toDto(OnboardingQuestionSectionEnum model);

    OnboardingModeEnum toModel(CompleteOnboardingAndGeneratePlanRequestDTO.ModeEnum dto);

    ProfileSnapshotDTO.HorizonEnum toDto(HorizonEnum model);

    GoalDTO toDto(GoalEnum model);

    // =====================================================================
    // Métodos Helper Personalizados
    // =====================================================================

    /**
     * Metodo calificado con @Named para gestionar la conversión del mapa 'factors'.
     * El mapa de origen tiene valores de tipo Object (Number, BigDecimal) y el
     * de destino requiere explícitamente BigDecimal.
     *
     * @param source El mapa de factores del modelo de dominio.
     * @return Un mapa con todos los valores convertidos a BigDecimal.
     */
    @Named("mapFactors")
    default Map<String, BigDecimal> mapFactors(Map<String, ?> source) {
        if (source == null) {
            return null;
        }
        Map<String, BigDecimal> target = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof BigDecimal bd) {
                target.put(entry.getKey(), bd);
            } else if (value instanceof Number num) {
                target.put(entry.getKey(), BigDecimal.valueOf(num.doubleValue()));
            }
        }
        return target;
    }
}