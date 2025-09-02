package com.revapp.planengine.infra.persistence.jpa.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert
@Table(name = "onboarding_block", schema = "plan_engine")
public class OnboardingBlockEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id; // e.g. BASICS, RISK_LIQUIDITY

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Builder.Default
    @OneToMany(
            mappedBy = "block",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<OnboardingQuestionEntity> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // helpers
    public void addQuestion(OnboardingQuestionEntity q) {
        questions.add(q);
        q.setBlock(this);
    }

    public void removeQuestion(OnboardingQuestionEntity q) {
        questions.remove(q);
        q.setBlock(null);
    }
}
