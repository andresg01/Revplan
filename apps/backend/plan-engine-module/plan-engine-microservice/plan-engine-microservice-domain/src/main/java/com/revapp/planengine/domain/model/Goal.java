package com.revapp.planengine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class Goal {
    private UUID id;
    private String name;               // "Fondo emergencia", "Viaje"
    private BigDecimal targetAmount;   // objetivo
    private LocalDate targetDate;      // opcional
    private Integer priority;          // 1..5
}
