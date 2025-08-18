package com.revapp.planengine.infra.persistence.jpa.entities;

import com.revapp.planengine.domain.enums.RecomputeReasonEnum;
import com.revapp.planengine.infra.persistence.jpa.converters.RecomputeReasonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recompute_event", schema = "plan_engine")
public class RecomputeEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Convert(converter = RecomputeReasonConverter.class)
    @Column(name = "reason", nullable = false, length = 32)
    private RecomputeReasonEnum reason; // balance_update | overspend | manual | sync

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
