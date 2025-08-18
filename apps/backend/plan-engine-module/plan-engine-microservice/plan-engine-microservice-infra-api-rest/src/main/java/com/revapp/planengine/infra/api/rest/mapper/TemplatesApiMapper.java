package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.model.*;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.api.dto.*;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = { PaginationApiMapper.class })
public interface TemplatesApiMapper {

    // Domain -> DTO
    default PlanTemplateDTO toDto(PlanTemplate t) {
        if (t == null) return null;
        PlanTemplateDTO dto = new PlanTemplateDTO();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setDescription(t.getDescription());
        dto.setDefaults(toDto(t.getDefaults()));
        dto.setConstraints(toDto(t.getConstraints()));
        dto.setApplicabilityRules(toDto(t.getApplicabilityRules()));
        dto.setWeights(toDto(t.getWeights()));
        dto.setActive(Boolean.TRUE.equals(t.getActive()));
        return dto;
    }

    default TemplateDefaultsDTO toDto(TemplateDefaults d) {
        if (d == null) return null;
        TemplateDefaultsDTO dto = new TemplateDefaultsDTO();
        dto.setSavingPct(d.getSavingPct());
        dto.setEmergencyMonths(d.getEmergencyMonths());
        dto.setEnvelopes(d.getEnvelopes());
        return dto;
    }

    default TemplateConstraintsDTO toDto(TemplateConstraints c) {
        if (c == null) return null;
        TemplateConstraintsDTO dto = new TemplateConstraintsDTO();
        dto.setMinSavingPct(c.getMinSavingPct());
        dto.setMaxSavingPct(c.getMaxSavingPct());
        dto.setMinEmergencyMonths(c.getMinEmergencyMonths());
        dto.setMaxEmergencyMonths(c.getMaxEmergencyMonths());
        return dto;
    }

    default TemplateApplicabilityRulesDTO toDto(TemplateApplicabilityRules r) {
        if (r == null) return null;
        TemplateApplicabilityRulesDTO dto = new TemplateApplicabilityRulesDTO();
        dto.setMinRisk(r.getMinRisk());
        dto.setMaxRisk(r.getMaxRisk());
        dto.setMaxDebtSeverity(r.getMaxDebtSeverity());
        dto.setMinLiquidityNeed(r.getMinLiquidityNeed());
        dto.setMinAge(r.getMinAge());
        dto.setMaxAge(r.getMaxAge());
        return dto;
    }

    default TemplateWeightsDTO toDto(TemplateWeights w) {
        if (w == null) return null;
        TemplateWeightsDTO dto = new TemplateWeightsDTO();
        dto.setRisk(w.getRisk());
        dto.setLiquidity(w.getLiquidity());
        dto.setDebt(w.getDebt());
        dto.setSavingsGap(w.getSavingsGap());
        dto.setAge(w.getAge());
        dto.setStability(w.getStability());
        return dto;
    }

    // DTO -> Domain (para create/update)
    default PlanTemplate toModel(PlanTemplateDTO dto) {
        if (dto == null) return null;
        return PlanTemplate.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .defaults(toModel(dto.getDefaults()))
                .constraints(toModel(dto.getConstraints()))
                .applicabilityRules(toModel(dto.getApplicabilityRules()))
                .weights(toModel(dto.getWeights()))
                .active(Boolean.TRUE.equals(dto.getActive()))
                .build();
    }

    default TemplateDefaults toModel(TemplateDefaultsDTO dto) {
        if (dto == null) return null;
        TemplateDefaults d = new TemplateDefaults();
        d.setSavingPct(dto.getSavingPct());
        d.setEmergencyMonths(dto.getEmergencyMonths());
        d.setEnvelopes(dto.getEnvelopes());
        return d;
    }

    default TemplateConstraints toModel(TemplateConstraintsDTO dto) {
        if (dto == null) return null;
        TemplateConstraints c = new TemplateConstraints();
        c.setMinSavingPct(dto.getMinSavingPct());
        c.setMaxSavingPct(dto.getMaxSavingPct());
        c.setMinEmergencyMonths(dto.getMinEmergencyMonths());
        c.setMaxEmergencyMonths(dto.getMaxEmergencyMonths());
        return c;
    }

    default TemplateApplicabilityRules toModel(TemplateApplicabilityRulesDTO dto) {
        if (dto == null) return null;
        TemplateApplicabilityRules r = new TemplateApplicabilityRules();
        r.setMinRisk(dto.getMinRisk());
        r.setMaxRisk(dto.getMaxRisk());
        r.setMaxDebtSeverity(dto.getMaxDebtSeverity());
        r.setMinLiquidityNeed(dto.getMinLiquidityNeed());
        r.setMinAge(dto.getMinAge());
        r.setMaxAge(dto.getMaxAge());
        return r;
    }

    default TemplateWeights toModel(TemplateWeightsDTO dto) {
        if (dto == null) return null;
        TemplateWeights w = new TemplateWeights();
        w.setRisk(dto.getRisk());
        w.setLiquidity(dto.getLiquidity());
        w.setDebt(dto.getDebt());
        w.setSavingsGap(dto.getSavingsGap());
        w.setAge(dto.getAge());
        w.setStability(dto.getStability());
        return w;
    }

    // Wrappers
    default TemplateDataDTO toData(PlanTemplate t) {
        TemplateDataDTO d = new TemplateDataDTO();
        d.setData(toDto(t));
        return d;
    }

    default TemplateListDataDTO toList(PageResult<PlanTemplate> page) {
        TemplateListDataDTO out = new TemplateListDataDTO();
        List<PlanTemplateDTO> list = new ArrayList<>();
        for (PlanTemplate t : page.items()) list.add(toDto(t));
        out.setData(list);
        out.setPagination(PaginationApiMapper.of(page));
        return out;
    }
}
