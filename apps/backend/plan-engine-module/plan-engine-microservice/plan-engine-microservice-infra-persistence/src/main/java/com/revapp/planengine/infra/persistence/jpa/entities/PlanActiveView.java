package com.revapp.planengine.infra.persistence.jpa.entities;

import com.revapp.planengine.domain.enums.PlanSourceEnum;
import com.revapp.planengine.domain.enums.PlanStatusEnum;
import com.revapp.planengine.infra.persistence.jpa.converters.PlanSourceConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Immutable
@Entity
@Subselect("select * from plan_engine.v_plan_active")
public class PlanActiveView {

    @Id
    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "active_version", nullable = false)
    private Integer activeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlanStatusEnum status;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", nullable = false)
    private Map<String, Object> params;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kpis", nullable = false)
    private Map<String, Object> kpis;

    @Column(name = "rationale")
    private String rationale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alerts", nullable = false)
    private List<String> alerts;

    @Convert(converter = PlanSourceConverter.class)
    @Column(name = "source", nullable = false, length = 16)
    private PlanSourceEnum source; // RULES | RULES_GPT

    @Column(name = "version_created_at", nullable = false)
    private LocalDateTime versionCreatedAt;
}
