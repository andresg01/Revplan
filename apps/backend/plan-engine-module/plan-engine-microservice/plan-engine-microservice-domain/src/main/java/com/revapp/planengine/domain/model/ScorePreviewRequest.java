package com.revapp.planengine.domain.model;

import lombok.*;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScorePreviewRequest {
    private String nameFilter;          // opcional (p.ej. filtrar plantillas por nombre)
    private ProfileSnapshot profile;    // opcional
    private AccountsAggregate accounts; // opcional
    private SpendSummary spend;         // opcional
    private DebtSummary debts;          // opcional
}
