package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.domain.model.GenerationResult;
import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.sse.SseChunkEvent;
import com.revplan.llm.domain.model.sse.SseFinishEvent;
import com.revplan.llm.domain.service.PlanJsonService;
import com.revplan.llm.infra.api.dto.GeneratePlanJsonRequestDTO;
import com.revplan.llm.infra.api.dto.PlanLLMOutputDataDTO;
import com.revplan.llm.infra.api.rest.mapper.PlansApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PlansController implements PlansApi {

    private final PlanJsonService planJsonService;
    private final PlansApiMapper plansApiMapper;

    @Override
    public ResponseEntity<PlanLLMOutputDataDTO> generatePlanJson(
            UUID xRequestID,
            GeneratePlanJsonRequestDTO generatePlanJsonRequestDTO,
            String acceptLanguage) {

        GeneratePlanJsonRequest req = plansApiMapper.toModel(generatePlanJsonRequestDTO);
        GenerationResult result = planJsonService.generate(req);

        PlanLLMOutputDataDTO body = plansApiMapper.toDataDto(result.getOutput());

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.getProviderRequestId() != null && !result.getProviderRequestId().isBlank()) {
            builder.header("X-Provider-Request-ID", result.getProviderRequestId());
        }
        return builder.body(body);
    }

    @Override
    public ResponseEntity<String> generatePlanJsonStream(GeneratePlanJsonRequestDTO generatePlanJsonRequestDTO) {
        GeneratePlanJsonRequest req = plansApiMapper.toModel(generatePlanJsonRequestDTO);

        StringBuilder sse = new StringBuilder();

        planJsonService.generateStream(
                req,
                (SseChunkEvent ev) -> {
                    sse.append("event: chunk\n");
                    sse.append("data: ").append(ev.getDelta() == null ? "" : ev.getDelta()).append("\n\n");
                },
                (SseFinishEvent fin) -> {
                    sse.append("event: finish\n");
                    sse.append("data: {");
                    boolean first = true;
                    if (fin.getProviderRequestId() != null) {
                        sse.append("\"providerRequestId\":\"").append(fin.getProviderRequestId()).append("\"");
                        first = false;
                    }
                    if (fin.getDurationMs() != null) {
                        if (!first) sse.append(",");
                        sse.append("\"durationMs\":").append(fin.getDurationMs());
                    }
                    sse.append("}\n\n");
                }
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(sse.toString());
    }
}
