package com.revplan.llm.infra.api.rest.controllers;

import com.revplan.llm.infra.api.dto.PromptTemplateDTO;
import com.revplan.llm.infra.api.dto.PromptTemplateDataDTO;
import com.revplan.llm.infra.api.dto.PromptTemplateListDataDTO;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public class PromptsController implements PromptsApi{

    @Override
    public ResponseEntity<PromptTemplateDataDTO> createPromptTemplate(UUID xRequestID, PromptTemplateDTO promptTemplateDTO, String acceptLanguage, String name, Integer offset, Integer limit) {
        return PromptsApi.super.createPromptTemplate(xRequestID, promptTemplateDTO, acceptLanguage, name, offset, limit);
    }

    @Override
    public ResponseEntity<Void> deletePromptTemplate(UUID xRequestID, String templateId, String acceptLanguage) {
        return PromptsApi.super.deletePromptTemplate(xRequestID, templateId, acceptLanguage);
    }

    @Override
    public ResponseEntity<PromptTemplateDataDTO> getPromptTemplateById(UUID xRequestID, String templateId, String acceptLanguage) {
        return PromptsApi.super.getPromptTemplateById(xRequestID, templateId, acceptLanguage);
    }

    @Override
    public ResponseEntity<PromptTemplateListDataDTO> getPromptTemplates(UUID xRequestID, String acceptLanguage, String name, Integer offset, Integer limit) {
        return PromptsApi.super.getPromptTemplates(xRequestID, acceptLanguage, name, offset, limit);
    }

    @Override
    public ResponseEntity<Void> updatePromptTemplate(UUID xRequestID, String templateId, PromptTemplateDTO promptTemplateDTO, String acceptLanguage) {
        return PromptsApi.super.updatePromptTemplate(xRequestID, templateId, promptTemplateDTO, acceptLanguage);
    }
}
