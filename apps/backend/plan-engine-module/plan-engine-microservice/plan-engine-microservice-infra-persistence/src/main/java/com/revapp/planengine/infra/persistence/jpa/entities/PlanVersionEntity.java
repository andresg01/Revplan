package com.revapp.planengine.infra.persistence.jpa.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.revapp.planengine.domain.enums.PlanSourceEnum;
import com.revapp.planengine.infra.persistence.jpa.converters.PlanSourceConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "plan_version", schema = "plan_engine")
public class PlanVersionEntity {

    @EmbeddedId
    private PlanVersionId id;

    @MapsId("planId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonBackReference
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private PlanTemplateEntity template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> params; // saving_pct, emergency_months, envelopes...

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kpis", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> kpis;

    @Column(name = "rationale")
    private String rationale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alerts", columnDefinition = "jsonb", nullable = false)
    private List<String> alerts;

    @Convert(converter = PlanSourceConverter.class)
    @Column(name = "source", nullable = false, length = 16)
    private PlanSourceEnum source; // 'rules' | 'rules_plus_gpt'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
