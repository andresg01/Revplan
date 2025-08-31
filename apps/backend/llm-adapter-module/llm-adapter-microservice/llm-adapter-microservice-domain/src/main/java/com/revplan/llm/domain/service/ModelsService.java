package com.revplan.llm.domain.service;

import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.model.ModelsData;

public interface ModelsService {
    ModelsData getModels(ModelProviderEnum provider);
}
