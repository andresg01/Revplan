package com.revapp.planengine.infra.persistence.jpa.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.revapp.planengine.domain.enums.PlanStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "plan",
        schema = "plan_engine",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_user", columnNames = "user_id")
)
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "active_version", nullable = false)
    private Integer activeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlanStatusEnum status;

    @Builder.Default
    @OneToMany(
            mappedBy = "plan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<PlanVersionEntity> versions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helpers bidireccionales
    public void addVersion(PlanVersionEntity version) {
        versions.add(version);
        version.setPlan(this);
    }

    public void removeVersion(PlanVersionEntity version) {
        versions.remove(version);
        version.setPlan(null);
    }
}
