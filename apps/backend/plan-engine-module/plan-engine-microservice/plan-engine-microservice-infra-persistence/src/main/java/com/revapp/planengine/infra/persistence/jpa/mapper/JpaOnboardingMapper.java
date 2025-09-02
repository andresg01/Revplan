package com.revapp.planengine.infra.persistence.jpa.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revapp.planengine.domain.enums.OnboardingQuestionSectionEnum;
import com.revapp.planengine.domain.model.OnboardingAnswer;
import com.revapp.planengine.domain.model.OnboardingQuestion;
import com.revapp.planengine.domain.model.OnboardingQuestionConstraints;
import com.revapp.planengine.domain.model.ProfileSnapshot;
import com.revapp.planengine.infra.persistence.jpa.entities.*;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre Entidades JPA de Onboarding y Modelos de Dominio.
 *
 * Se utiliza una clase abstracta para permitir la inyección de ObjectMapper,
 * necesario para manejar la (de)serialización de campos JSONB.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED
)
public abstract class JpaOnboardingMapper {

    // ObjectMapper es inyectado por Spring para manejar conversiones JSON <-> Objeto
    @Autowired
    protected ObjectMapper objectMapper;

    // =====================================================================
    // Profile Snapshot (Entidad <-> Modelo)
    // =====================================================================

    public abstract ProfileSnapshot toModel(OnboardingProfileSnapshotEntity entity);

    @AfterMapping
    protected void mapJsonToProfileSnapshot(OnboardingProfileSnapshotEntity entity, @MappingTarget ProfileSnapshot model) {
        if (entity.getSnapshot() != null) {
            ProfileSnapshot snapshotData = objectMapper.convertValue(entity.getSnapshot(), ProfileSnapshot.class);
            // Copiamos las propiedades del objeto deserializado al modelo destino
            // Esto se podría hacer con un BeanUtils o un mapper adicional si el objeto es muy complejo.
            // Por simplicidad, asumimos que objectMapper lo gestiona bien.
            // (Esta es una forma simplificada; en un caso real, podrías necesitar un mapeo más detallado)
            model.setAge(snapshotData.getAge());
            model.setDependents(snapshotData.getDependents());
            model.setHorizon(snapshotData.getHorizon());
            model.setGoals(snapshotData.getGoals());
            model.setIncomeNet(snapshotData.getIncomeNet());
            model.setExpensesFixed(snapshotData.getExpensesFixed());
            model.setExpensesVariable(snapshotData.getExpensesVariable());
            model.setRiskScore(snapshotData.getRiskScore());
            model.setStabilityIndex(snapshotData.getStabilityIndex());
            model.setLiquidityNeed(snapshotData.getLiquidityNeed());
            model.setDebtSeverity(snapshotData.getDebtSeverity());
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "userId", target = "userId")
    public abstract OnboardingProfileSnapshotEntity toEntity(ProfileSnapshot model, UUID userId);

    @AfterMapping
    protected void mapProfileSnapshotToJson(ProfileSnapshot model, @MappingTarget OnboardingProfileSnapshotEntity entity) {
        if (model != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshotMap = objectMapper.convertValue(model, Map.class);
            entity.setSnapshot(snapshotMap);
        }
    }

    // =====================================================================
    // Question (Entidad -> Modelo)
    // =====================================================================

    @Mapping(source = "label", target = "text")
    @Mapping(source = "qtype", target = "type")
    // Los siguientes campos se mapean en el método @AfterMapping
    @Mapping(target = "section", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "constraints", ignore = true)
    public abstract OnboardingQuestion toModel(OnboardingQuestionEntity entity);

    @AfterMapping
    protected void completeQuestionMapping(OnboardingQuestionEntity entity, @MappingTarget OnboardingQuestion model) {
        // 1. Mapear el Bloque a la Sección del dominio
        if (entity.getBlock() != null && entity.getBlock().getId() != null) {
            model.setSection(mapBlockIdToSection(entity.getBlock().getId()));
        }

        // 2. Mapear las opciones de la entidad a una lista de strings
        if (entity.getOptions() != null) {
            model.setOptions(mapOptionsEntityToString(entity.getOptions()));
        }

        // 3. Convertir el mapa 'validations' a un objeto OnboardingQuestionConstraints
        if (entity.getValidations() != null) {
            model.setConstraints(objectMapper.convertValue(entity.getValidations(), OnboardingQuestionConstraints.class));
        }
    }

    // =====================================================================
    // Answer (Entidad/Vista -> Modelo)
    // =====================================================================

    /**
     * Mapea desde la vista que contiene la última respuesta de un usuario para una pregunta.
     */
    @Mapping(source = "questionId", target = "id")
    public abstract OnboardingAnswer toModel(OnboardingAnswersLatestView view);

    /**
     * Mapea una lista de vistas a una lista de modelos de respuesta.
     */
    public abstract List<OnboardingAnswer> toModelList(List<OnboardingAnswersLatestView> views);

    /**
     * Mapea desde la entidad principal de respuestas.
     */
    @Mapping(source = "question.id", target = "id")
    public abstract OnboardingAnswer toModel(OnboardingAnswerEntity entity);


    // =====================================================================
    // Answer (Modelo -> Entidad)
    // =====================================================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "session", ignore = true) // La sesión se asigna en la capa de servicio
    @Mapping(target = "latest", ignore = true)  // La lógica de 'latest' se gestiona en el servicio
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "model.value", target = "value")
    @Mapping(source = "questionEntity", target = "question")
    public abstract OnboardingAnswerEntity toAnswerEntity(OnboardingAnswer model, UUID userId, OnboardingQuestionEntity questionEntity);


    // =====================================================================
    // Métodos Helper
    // =====================================================================

    /**
     * Convierte el ID de un bloque de la BBDD a un Enum de sección del dominio.
     * Esta es una lógica de negocio que reside en el mapper.
     */
    protected OnboardingQuestionSectionEnum mapBlockIdToSection(String blockId) {
        if (blockId == null) return OnboardingQuestionSectionEnum.OTROS;
        return switch (blockId) {
            case "BASICS" -> OnboardingQuestionSectionEnum.PERFIL;
            case "INCOME_EXPENSES" -> OnboardingQuestionSectionEnum.GASTOS; // O INGRESOS, depende de la pregunta
            case "GOALS" -> OnboardingQuestionSectionEnum.OBJETIVOS;
            // RISK_LIQUIDITY no tiene un mapeo directo, podría ser PERFIL o OTROS
            case "RISK_LIQUIDITY" -> OnboardingQuestionSectionEnum.PERFIL;
            default -> OnboardingQuestionSectionEnum.OTROS;
        };
    }

    /**
     * Extrae el campo 'value' de la lista de entidades de opciones.
     */
    protected List<String> mapOptionsEntityToString(List<OnboardingQuestionOptionEntity> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(option -> String.valueOf(option.getValue()))
                .collect(Collectors.toList());
    }
}