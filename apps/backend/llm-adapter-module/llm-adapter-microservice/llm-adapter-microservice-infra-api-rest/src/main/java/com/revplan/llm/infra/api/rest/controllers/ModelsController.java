package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.infra.api.dto.GetModels200ResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class ModelsController implements ModelsApi{

    @Override
    public ResponseEntity<GetModels200ResponseDTO> getModels(UUID xRequestID, String acceptLanguage) {
        return ModelsApi.super.getModels(xRequestID, acceptLanguage);
    }
}
