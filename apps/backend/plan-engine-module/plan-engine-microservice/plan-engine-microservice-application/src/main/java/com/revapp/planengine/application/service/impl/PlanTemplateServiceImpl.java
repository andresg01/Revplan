package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.exceptions.NotFoundException;
import com.revapp.planengine.domain.model.PlanTemplate;
import com.revapp.planengine.domain.repository.PlanTemplateRepository;
import com.revapp.planengine.domain.service.PlanTemplateService;
import com.revapp.planengine.domain.utils.PageRequest;
import com.revapp.planengine.domain.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanTemplateServiceImpl implements PlanTemplateService {

    private final PlanTemplateRepository repository;

    @Override
    public PageResult<PlanTemplate> list(String nameLike, PageRequest page) {
        log.debug("PlanTemplateService.list(nameLike='{}', page={})", nameLike, page);
        if (page == null) {
            log.warn("PlanTemplateService.list -> page is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "page is required");
        }
        Instant t0 = Instant.now();
        PageResult<PlanTemplate> result = repository.findAll(nameLike, page);
        log.info("PlanTemplateService.list(nameLike='{}') -> {} items (total={}) in {}",
                nameLike, result.items().size(), result.total(), Duration.between(t0, Instant.now()));
        return result;
    }

    @Override
    public Optional<PlanTemplate> getById(String id) {
        log.debug("PlanTemplateService.getById(id={})", id);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateService.getById -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        Optional<PlanTemplate> opt = repository.findById(id);
        log.info("PlanTemplateService.getById(id={}) -> {} in {}",
                id, opt.isPresent() ? "FOUND" : "NOT_FOUND", Duration.between(t0, Instant.now()));
        return opt;
    }

    /** Convenience for controllers that want 404 with standard body. */
    public PlanTemplate getRequiredById(String id) {
        log.debug("PlanTemplateService.getRequiredById(id={})", id);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateService.getRequiredById -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        PlanTemplate t = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + id));
        log.info("PlanTemplateService.getRequiredById(id={}) -> FOUND in {}", id, Duration.between(t0, Instant.now()));
        return t;
    }

    @Override
    public PlanTemplate create(PlanTemplate template) {
        log.debug("PlanTemplateService.create(id={}, name={})",
                template != null ? template.getId() : null,
                template != null ? template.getName() : null);

        if (template == null) {
            log.warn("PlanTemplateService.create -> template is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template is required");
        }
        if (template.getId() == null || template.getId().isBlank()) {
            log.warn("PlanTemplateService.create -> template.id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "template.id is required");
        }
        if (template.getName() == null || template.getName().isBlank()) {
            log.warn("PlanTemplateService.create -> template.name is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "template.name is required");
        }

        if (repository.findById(template.getId()).isPresent()) {
            log.warn("PlanTemplateService.create -> duplicate template id={}", template.getId());
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Template already exists: " + template.getId());
        }

        Instant t0 = Instant.now();
        PlanTemplate saved = repository.save(template);
        log.info("PlanTemplateService.create(id={}) -> CREATED in {}", saved.getId(), Duration.between(t0, Instant.now()));
        return saved;
    }

    @Override
    public Optional<PlanTemplate> update(String id, PlanTemplate template) {
        log.debug("PlanTemplateService.update(id={}, bodyId={})", id, template != null ? template.getId() : null);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateService.update -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        if (template == null) {
            log.warn("PlanTemplateService.update -> template is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "template is required");
        }

        // 404 if it doesn't exist
        if (repository.findById(id).isEmpty()) {
            log.warn("PlanTemplateService.update -> template not found: {}", id);
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + id);
        }

        template.setId(id);
        Instant t0 = Instant.now();
        PlanTemplate saved = repository.save(template);
        log.info("PlanTemplateService.update(id={}) -> UPDATED in {}", id, Duration.between(t0, Instant.now()));
        return Optional.of(saved);
    }

    @Override
    public void delete(String id) {
        log.debug("PlanTemplateService.delete(id={})", id);
        if (id == null || id.isBlank()) {
            log.warn("PlanTemplateService.delete -> id is blank");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "id is required");
        }
        Instant t0 = Instant.now();
        repository.deleteById(id); // adapter throws 404 if not exists
        log.info("PlanTemplateService.delete(id={}) -> NO_CONTENT in {}", id, Duration.between(t0, Instant.now()));
    }
}
