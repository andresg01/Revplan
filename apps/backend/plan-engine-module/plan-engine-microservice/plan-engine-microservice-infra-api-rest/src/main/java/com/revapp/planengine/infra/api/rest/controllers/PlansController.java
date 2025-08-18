package com.revapp.planengine.infra.api.rest.controllers;

import com.revapp.planengine.domain.model.Plan;
import com.revapp.planengine.domain.model.PlanSimulationData;
import com.revapp.planengine.domain.service.*;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.infra.api.dto.*;
import com.revapp.planengine.infra.api.rest.mapper.PlanApiMapper;
import com.revapp.planengine.infra.api.rest.mapper.RequestsApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PlansController implements PlanEngineApi {

    private final PlanService planService;
    private final PlanGenerationService planGenerationService;
    private final PlanRecomputeService planRecomputeService;
    private final PlanSimulationService planSimulationService;

    private final PlanApiMapper planApiMapper;
    private final RequestsApiMapper requestsApiMapper;

    @Override
    public ResponseEntity<PlanListDataDTO> getPlans(UUID xRequestID, UUID userId, String acceptLanguage,
                                                    Boolean activeOnly, Integer offset, Integer limit) {
        var pageReq = PageRequest.of(offset, limit);
        var page = planService.getByUser(userId, Boolean.TRUE.equals(activeOnly), pageReq);
        return ResponseEntity.ok(planApiMapper.toList(page));
    }

    @Override
    public ResponseEntity<PlanDataDTO> getPlanById(UUID xRequestID, UUID planId, String acceptLanguage) {
        Optional<Plan> opt = planService.getById(planId);
        return opt.map(p -> ResponseEntity.ok(planApiMapper.toData(p)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<Void> updatePlan(UUID xRequestID, UUID planId,
                                           UpdatePlanRequestDTO updatePlanRequestDTO, String acceptLanguage) {
        var adjustments = requestsApiMapper.toModel(updatePlanRequestDTO);
        var updated = planService.getById(planId).map(existing -> {
            existing.setAdjustments(adjustments);
            return planService.save(existing);
        });
        return updated.isPresent()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Override
    public ResponseEntity<RecomputePlan202ResponseDTO> recomputePlan(UUID xRequestID, UUID planId,
                                                                     RecomputePlanRequestDTO recomputePlanRequestDTO, String acceptLanguage) {

        var reason = requestsApiMapper.toModel(
                recomputePlanRequestDTO != null ? recomputePlanRequestDTO.getReason() : null);

        UUID eventId = planRecomputeService.enqueueRecompute(planId, reason);

        // Devolvemos 202 y, si existe setter compatible, incluimos el id por reflexión.
        RecomputePlan202ResponseDTO body = new RecomputePlan202ResponseDTO();
        try {
            var m = RecomputePlan202ResponseDTO.class.getMethod("setId", UUID.class);
            m.invoke(body, eventId);
        } catch (Exception ignored) {
            try {
                var m = RecomputePlan202ResponseDTO.class.getMethod("setEventId", UUID.class);
                m.invoke(body, eventId);
            } catch (Exception ignored2) { /* cuerpo vacío si no hay campos */ }
        }
        return ResponseEntity.accepted().body(body);
    }

    @Override
    public ResponseEntity<PlanSimulationDataDTO> simulatePlan(UUID xRequestID, UUID planId,
                                                              PlanAdjustmentsDTO planAdjustmentsDTO, String acceptLanguage) {

        var adj = planApiMapper.toModel(planAdjustmentsDTO);
        PlanSimulationData sim = planSimulationService.simulate(planId, adj);
        return ResponseEntity.ok(requestsApiMapper.toDto(sim));
    }

    @Override
    public ResponseEntity<PlanDataDTO> generatePlan(UUID xRequestID,
                                                    GeneratePlanRequestDTO generatePlanRequestDTO,
                                                    String acceptLanguage) {
        var req = requestsApiMapper.toModel(generatePlanRequestDTO);
        var plan = planGenerationService.generate(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(planApiMapper.toData(plan));
    }
}
