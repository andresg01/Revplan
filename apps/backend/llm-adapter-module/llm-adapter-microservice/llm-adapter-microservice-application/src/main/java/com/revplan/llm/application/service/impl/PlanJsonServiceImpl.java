package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.model.GenerationResult;
import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.sse.SseChunkEvent;
import com.revplan.llm.domain.model.sse.SseFinishEvent;
import com.revplan.llm.domain.repository.PlanJsonProviderRepository;
import com.revplan.llm.domain.service.PlanJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PlanJsonServiceImpl implements PlanJsonService {

    private final PlanJsonProviderRepository providerRepository;

    @Override
    public GenerationResult generate(GeneratePlanJsonRequest request) {
        return providerRepository.generate(request);
    }

    @Override
    public void generateStream(GeneratePlanJsonRequest request,
                               Consumer<SseChunkEvent> onChunk,
                               Consumer<SseFinishEvent> onFinish) {
        providerRepository.generateStream(request, onChunk, onFinish);
    }
}
