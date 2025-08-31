package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.model.ModelSpec;
import com.revplan.llm.domain.model.ModelsData;
import com.revplan.llm.domain.repository.ModelsRepository;
import com.revplan.llm.domain.service.ModelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelsServiceImpl implements ModelsService {

    private final ModelsRepository modelsRepository;

    @Override
    public ModelsData getModels(ModelProviderEnum provider) {
        ModelProviderEnum prov = (provider == null ? ModelProviderEnum.OPENAI : provider);
        List<ModelSpec> list = modelsRepository.fetchModels(prov);
        return ModelsData.builder()
                .data(list)
                .build();
    }
}
