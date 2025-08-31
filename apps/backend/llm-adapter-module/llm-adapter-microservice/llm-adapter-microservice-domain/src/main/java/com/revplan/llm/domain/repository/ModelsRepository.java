package com.revplan.llm.domain.repository;

import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.model.ModelSpec;

import java.util.List;

public interface ModelsRepository {
    List<ModelSpec> fetchModels(ModelProviderEnum provider);
}
