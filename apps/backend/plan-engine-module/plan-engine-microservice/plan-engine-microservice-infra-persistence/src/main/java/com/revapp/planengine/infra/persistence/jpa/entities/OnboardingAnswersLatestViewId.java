package com.revapp.planengine.infra.persistence.jpa.entities;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OnboardingAnswersLatestViewId implements Serializable {
    private UUID userId;
    private String questionId;
}
