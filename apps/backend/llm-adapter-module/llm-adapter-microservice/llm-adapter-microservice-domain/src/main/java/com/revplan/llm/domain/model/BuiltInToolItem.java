package com.revplan.llm.domain.model;

import javax.tools.Tool;

import com.revplan.llm.domain.enums.ToolEnum;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuiltInToolItem {
    private ToolEnum tool;
    private Pricing pricing;
}