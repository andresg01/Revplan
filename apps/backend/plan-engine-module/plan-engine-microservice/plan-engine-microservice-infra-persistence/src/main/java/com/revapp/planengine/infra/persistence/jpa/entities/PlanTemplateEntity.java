package com.revapp.planengine.infra.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert // respeta DEFAULT de la BBDD y omite columnas nulas en INSERT
@Table(name = "plan_template", schema = "plan_engine")
public class PlanTemplateEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> paramsSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoring_weights", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> scoringWeights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints_doc", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> constraintsDoc;

    @Column(name = "protected", nullable = false)
    private Boolean protectedTemplate;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (version == null) version = 1; // <-- evita el 23502 de tu traza

        if (paramsSchema == null) {
            paramsSchema = new HashMap<>();
            paramsSchema.put("type", "object");
            paramsSchema.put("properties", Map.of(
                    "saving_pct", Map.of("type", "number", "minimum", 0, "maximum", 1),
                    "emergency_months", Map.of("type", "integer", "minimum", 0, "maximum", 24),
                    "envelopes", Map.of("type", "object", "additionalProperties",
                            Map.of("type", "number", "minimum", 0, "maximum", 1))
            ));
            paramsSchema.put("required", java.util.List.of("saving_pct", "emergency_months"));
        }

        if (scoringWeights == null) {
            scoringWeights = new HashMap<>();
            scoringWeights.put("risk", 0.20);
            scoringWeights.put("liquidity", 0.20);
            scoringWeights.put("debt", 0.20);
            scoringWeights.put("gap", 0.20); // en dominio será savingsGap
            scoringWeights.put("age", 0.10);
            scoringWeights.put("stability", 0.10);
        }

        if (constraintsDoc == null) constraintsDoc = new HashMap<>();
        if (protectedTemplate == null) protectedTemplate = Boolean.FALSE;
        if (checksum == null) checksum = "chk-" + UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
