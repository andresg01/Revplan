package com.revapp.planengine.infra.api.rest.mapper;

import com.revapp.planengine.domain.enums.PlanStatusEnum;
import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.PlanAdjustments;
import com.revapp.planengine.domain.model.PlanKPIs;
import com.revapp.planengine.domain.utils.PageResult;
import com.revapp.planengine.infra.api.dto.*;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", uses = { PaginationApiMapper.class })
public interface PlanApiMapper {

    // ---- Plan <-> DTO ----
    @Mapping(target = "id",          source = "id")
    @Mapping(target = "userId",      source = "userId")
    @Mapping(target = "planVersion", source = "planVersion")
    @Mapping(target = "status",      source = "status", qualifiedByName = "mapStatusToDto")
    @Mapping(target = "templateId",  source = "templateId")
    @Mapping(target = "adjustments", source = "adjustments")
    @Mapping(target = "rationale",   source = "rationale")
    @Mapping(target = "alerts",      source = "alerts")
    @Mapping(target = "kpis",        source = "kpis")
    PlanDTO toDto(Plan model);

    default PlanDataDTO toData(Plan model) {
        if (model == null) return null;
        PlanDataDTO dto = new PlanDataDTO();
        dto.setData(toDto(model));
        return dto;
    }

    default PlanListDataDTO toList(PageResult<Plan> page) {
        PlanListDataDTO out = new PlanListDataDTO();
        List<PlanDTO> list = new ArrayList<>();
        for (Plan p : page.items()) list.add(toDto(p));
        out.setData(list);
        out.setPagination(PaginationApiMapper.of(page));
        return out;
    }

    // ---- nested: Adjustments/KPIs ----
    default PlanAdjustmentsDTO toDto(PlanAdjustments m) {
        if (m == null) return null;
        PlanAdjustmentsDTO dto = new PlanAdjustmentsDTO();
        dto.setSavingPct(m.getSavingPct());
        dto.setEmergencyMonths(m.getEmergencyMonths());
        dto.setEnvelopes(m.getEnvelopes());
        return dto;
    }

    default PlanAdjustments toModel(PlanAdjustmentsDTO dto) {
        if (dto == null) return null;
        PlanAdjustments m = new PlanAdjustments();
        m.setSavingPct(dto.getSavingPct());
        m.setEmergencyMonths(dto.getEmergencyMonths());
        m.setEnvelopes(dto.getEnvelopes());
        return m;
    }

    default PlanKPIsDTO toDto(PlanKPIs k) {
        if (k == null) return null;
        PlanKPIsDTO dto = new PlanKPIsDTO();
        dto.setSavingRate(k.getSavingRate());
        dto.setRunwayMonths(k.getRunwayMonths());
        dto.setBudgetCompliance(k.getBudgetCompliance());
        return dto;
    }

    default PlanKPIs toModel(PlanKPIsDTO dto) {
        if (dto == null) return null;
        return new PlanKPIs(dto.getSavingRate(), dto.getRunwayMonths(), dto.getBudgetCompliance());
    }

    // ---- enums ----
    @Named("mapStatusToDto")
    default PlanStatusDTO mapStatusToDto(PlanStatusEnum st) {
        if (st == null) return null;
        return switch (st) {
            case DRAFT -> PlanStatusDTO.DRAFT;
            case ACTIVE -> PlanStatusDTO.ACTIVE;
            case ARCHIVED -> PlanStatusDTO.ARCHIVED;
            default -> PlanStatusDTO.DRAFT;
        };
    }
}
