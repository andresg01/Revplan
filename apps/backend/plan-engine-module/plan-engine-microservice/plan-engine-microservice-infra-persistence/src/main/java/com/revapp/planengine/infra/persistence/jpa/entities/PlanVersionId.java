package com.revapp.planengine.infra.persistence.jpa.entities;

import lombok.*;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PlanVersionId implements Serializable {
    private UUID planId;
    private Integer version;
}
