package com.revapp.planengine.infra.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@org.hibernate.annotations.Subselect("select * from plan_engine.v_onboarding_profile_preview")
public class OnboardingProfilePreviewView {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> profileSnapshot;
}
