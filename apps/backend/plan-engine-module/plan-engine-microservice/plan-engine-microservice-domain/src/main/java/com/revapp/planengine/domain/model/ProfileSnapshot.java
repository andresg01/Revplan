package com.revapp.planengine.domain.model;

import com.revapp.planengine.domain.enums.GoalEnum;
import com.revapp.planengine.domain.enums.HorizonEnum;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileSnapshot {
    private Integer age;                 // opcional
    private BigDecimal stabilityIndex;   // nuevo (DTO)
    private BigDecimal liquidityNeed;
    private Integer riskScore;
    private BigDecimal debtSeverity;     // opcional
    private BigDecimal incomeNet;        // en DTO es Double → mapea a BigDecimal aquí
    private BigDecimal expensesFixed;    // idem
    private BigDecimal expensesVariable; // idem
    private Integer dependents;          // opcional
    private HorizonEnum horizon;         // opcional
    @Builder.Default
    private List<GoalEnum> goals = new ArrayList<>(); // DTO usa enum, no objeto complejo
}
