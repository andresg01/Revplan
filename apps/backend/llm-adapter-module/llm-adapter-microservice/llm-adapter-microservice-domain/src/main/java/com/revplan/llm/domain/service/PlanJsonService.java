package com.revplan.llm.domain.service;

import com.revplan.llm.domain.model.GenerationResult;

import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.sse.SseChunkEvent;
import com.revplan.llm.domain.model.sse.SseFinishEvent;

import java.util.function.Consumer;

public interface PlanJsonService {
    GenerationResult generate(GeneratePlanJsonRequest request);

    void generateStream(GeneratePlanJsonRequest request,
                        Consumer<SseChunkEvent> onChunk,
                        Consumer<SseFinishEvent> onFinish);
}
