package com.revplan.llm.domain.repository;

import com.revplan.llm.domain.model.GenerationResult;
import com.revplan.llm.domain.model.GeneratePlanJsonRequest;
import com.revplan.llm.domain.model.sse.SseChunkEvent;
import com.revplan.llm.domain.model.sse.SseFinishEvent;


import java.util.function.Consumer;

public interface PlanJsonProviderRepository {
    GenerationResult generate(GeneratePlanJsonRequest request);

    void generateStream(GeneratePlanJsonRequest request,
                        Consumer<SseChunkEvent> onChunk,
                        Consumer<SseFinishEvent> onFinish);
}
