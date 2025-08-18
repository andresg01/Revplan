package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.PlanSourceEnum;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePlanRequest {
    private UUID userId;

    /** Modo del motor: RULES o RULES_PLUS_GPT (equivalente a DTO ModeEnum). */
    private PlanSourceEnum mode;

    /** Forzar una plantilla concreta (opcional). */
    private String forceTemplateId;

    /** Entradas del cálculo (perfil + agregados). */
    private ProfileSnapshot profile;       // requerido en DTO
    private AccountsAggregate accounts;    // opcional
    private SpendSummary spend;            // opcional
    private DebtSummary debts;             // opcional

    /** Pedir explicación del razonamiento (por defecto true como en el DTO). */
    @Builder.Default
    private boolean explain = true;
}
