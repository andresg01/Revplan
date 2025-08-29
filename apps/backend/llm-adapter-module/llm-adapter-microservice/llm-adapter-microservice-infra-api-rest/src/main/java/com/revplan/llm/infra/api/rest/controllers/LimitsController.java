package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.infra.api.dto.GetLimits200ResponseDTO;
import org.springframework.http.ResponseEntity;

public class LimitsController implements LimitsApi{

    @Override
    public ResponseEntity<GetLimits200ResponseDTO> getLimits() {
        return LimitsApi.super.getLimits();
    }
}
