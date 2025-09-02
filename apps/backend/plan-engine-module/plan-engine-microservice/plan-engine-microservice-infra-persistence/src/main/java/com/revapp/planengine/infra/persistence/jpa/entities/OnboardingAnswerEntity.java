package com.revapp.planengine.infra.persistence.jpa.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert
@Table(
        name = "onboarding_answer",
        schema = "plan_engine",
        indexes = {
                @Index(name = "idx_onb_answer_user_question", columnList = "user_id, question_id, created_at DESC"),
                @Index(name = "idx_onb_answer_session", columnList = "session_id")
        }
)
public class OnboardingAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Puede ser NULL si la respuesta no está ligada a una sesión activa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @JsonBackReference
    private OnboardingSessionEntity session;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private OnboardingQuestionEntity question;

    // JSON arbitrario: número, string, boolean, array o objeto
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", columnDefinition = "jsonb", nullable = false)
    private Object value;

    @Column(name = "is_latest", nullable = false)
    private Boolean latest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
