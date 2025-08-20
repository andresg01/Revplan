/*******************************************************************************
 *
 * Autor: Andres Garcia
 *
 * © Axpe Consulting S.L. 2025. Todos los derechos reservados.
 *
 ******************************************************************************/

package com.revapp.planengine.infra.api.rest.controllers;

import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.service.PlanTemplateService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.infra.api.dto.PlanTemplateDTO;
import com.revapp.planengine.infra.api.dto.TemplateDataDTO;
import com.revapp.planengine.infra.api.dto.TemplateListDataDTO;
import com.revapp.planengine.infra.api.rest.mapper.TemplatesApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TemplatesController implements TemplatesApi {

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
        // Si no existe, el service lanza NotFoundException → tu @ControllerAdvice construye el body 404
        PlanTemplate model = ((com.revapp.planengine.application.service.impl.PlanTemplateServiceImpl) planTemplateService)
                .getRequiredById(templateId);
        return ResponseEntity.ok(templatesApiMapper.toData(model));
    }

    @Override
    public ResponseEntity<TemplateDataDTO> createTemplate(UUID xRequestID, PlanTemplateDTO planTemplateDTO,
                                                          String acceptLanguage, String name, Integer offset, Integer limit) {
        var model = templatesApiMapper.toModel(planTemplateDTO);
        var saved = planTemplateService.create(model); // si duplica → BusinessException(DUPLICATE_RESOURCE)
        return ResponseEntity.status(HttpStatus.CREATED).body(templatesApiMapper.toData(saved));
    }

    @Override
    public ResponseEntity<Void> updateTemplate(UUID xRequestID, String templateId,
                                               PlanTemplateDTO planTemplateDTO, String acceptLanguage) {
        var model = templatesApiMapper.toModel(planTemplateDTO);
        // Si no existe, el service lanza NotFoundException → body 404 del @ControllerAdvice
        planTemplateService.update(templateId, model)
                .orElseThrow(); // no debería ocurrir porque el service ya lanzó 404 antes
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(UUID xRequestID, String templateId, String acceptLanguage) {
        // Si no existe, el adapter lanza NotFoundException → body 404
        planTemplateService.delete(templateId);
        return ResponseEntity.noContent().build();
    }
}
