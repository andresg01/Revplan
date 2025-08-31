package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.domain.enums.ModelProviderEnum;
import com.revplan.llm.domain.model.ModelsData;
import com.revplan.llm.domain.service.ModelsService;
import com.revplan.llm.infra.api.dto.GetModels200ResponseDTO;
import com.revplan.llm.infra.api.rest.mapper.ModelsApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ModelsController implements ModelsApi {

    private final ModelsService modelsService;
    private final ModelsApiMapper modelsApiMapper;

    @Override
    public ResponseEntity<GetModels200ResponseDTO> getModels(UUID xRequestID, String acceptLanguage) {
        ModelsData data = modelsService.getModels(ModelProviderEnum.OPENAI);
        return ResponseEntity.ok(modelsApiMapper.toListDto(data));
    }
}
