package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.infra.api.dto.PlanLLMOutputSchemaDocDTO;
import com.revplan.llm.infra.api.dto.ValidateJsonAgainstSchema200ResponseDTO;
import com.revplan.llm.infra.api.dto.ValidationTargetDTO;
import org.springframework.http.ResponseEntity;

public class SchemasController implements SchemasApi{

    @Override
    public ResponseEntity<PlanLLMOutputSchemaDocDTO> getPlanOutputSchema() {
        return SchemasApi.super.getPlanOutputSchema();
    }

    @Override
    public ResponseEntity<ValidateJsonAgainstSchema200ResponseDTO> validateJsonAgainstSchema(ValidationTargetDTO validationTargetDTO) {
        return SchemasApi.super.validateJsonAgainstSchema(validationTargetDTO);
    }
}
