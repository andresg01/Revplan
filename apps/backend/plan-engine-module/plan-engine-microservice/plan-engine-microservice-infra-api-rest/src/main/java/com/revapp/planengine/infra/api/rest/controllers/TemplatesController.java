package com.revapp.planengine.infra.api.rest.controllers;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.service.PlanTemplateService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.infra.api.dto.PlanTemplateDTO;
import com.revapp.planengine.infra.api.dto.TemplateDataDTO;
import com.revapp.planengine.infra.api.dto.TemplateListDataDTO;
import com.revapp.planengine.infra.api.rest.mapper.TemplatesApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TemplatesController implements PlanEngineApi {

    private final PlanTemplateService planTemplateService;
    private final TemplatesApiMapper templatesApiMapper;

    @Override
    public ResponseEntity<TemplateListDataDTO> getTemplates(UUID xRequestID, String acceptLanguage,
                                                            String name, Integer offset, Integer limit) {
        var page = planTemplateService.list(name, PageRequest.of(offset, limit));
        return ResponseEntity.ok(templatesApiMapper.toList(page));
    }

    @Override
    public ResponseEntity<TemplateDataDTO> getTemplateById(UUID xRequestID, String templateId, String acceptLanguage) {
        Optional<PlanTemplate> opt = planTemplateService.getById(templateId);
        return opt.map(t -> ResponseEntity.ok(templatesApiMapper.toData(t)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<TemplateDataDTO> createTemplate(UUID xRequestID, PlanTemplateDTO planTemplateDTO,
                                                          String acceptLanguage, String name, Integer offset, Integer limit) {
        var model = templatesApiMapper.toModel(planTemplateDTO);
        var saved = planTemplateService.create(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(templatesApiMapper.toData(saved));
    }

    @Override
    public ResponseEntity<Void> updateTemplate(UUID xRequestID, String templateId,
                                               PlanTemplateDTO planTemplateDTO, String acceptLanguage) {
        var model = templatesApiMapper.toModel(planTemplateDTO);
        var updated = planTemplateService.update(templateId, model);
        return updated.isPresent() ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(UUID xRequestID, String templateId, String acceptLanguage) {
        planTemplateService.delete(templateId);
        return ResponseEntity.noContent().build();
    }
}
