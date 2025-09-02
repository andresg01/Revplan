package com.revapp.planengine.infra.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@IdClass(OnboardingAnswersLatestViewId.class)
@org.hibernate.annotations.Subselect("select * from plan_engine.v_onboarding_answers_latest")
public class OnboardingAnswersLatestView {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "question_id", nullable = false, length = 64)
    private String questionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", nullable = false, columnDefinition = "jsonb")
    private Object value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
