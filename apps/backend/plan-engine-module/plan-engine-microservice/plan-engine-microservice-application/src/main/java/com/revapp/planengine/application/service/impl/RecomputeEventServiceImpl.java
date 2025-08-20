package com.revapp.planengine.application.service.impl;

import com.revapp.planengine.domain.enums.ErrorCode;
import com.revapp.planengine.domain.exceptions.BusinessException;
import com.revapp.planengine.domain.model.RecomputeEvent;
import com.revapp.planengine.domain.repository.RecomputeEventRepository;
import com.revapp.planengine.domain.service.RecomputeEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecomputeEventServiceImpl implements RecomputeEventService {

    private final RecomputeEventRepository repository;

    @Override
    public RecomputeEvent append(RecomputeEvent event) {
        log.debug("RecomputeEventService.append(planId={}, reason={}, hasPayload={})",
                event != null ? event.getPlanId() : null,
                event != null ? event.getReason() : null,
                event != null && event.getPayload() != null);

        // Business guards (lightweight)
        if (event == null) {
            log.warn("RecomputeEventService.append -> event is null");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "event is required");
        }
        if (event.getPlanId() == null) {
            log.warn("RecomputeEventService.append -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        if (event.getReason() == null) {
            log.warn("RecomputeEventService.append -> reason is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "reason is required");
        }

        // Normalize null payload → {}
        if (event.getPayload() == null) {
            log.debug("RecomputeEventService.append(planId={}) -> normalizing null payload to empty map", event.getPlanId());
            event.setPayload(Map.of());
        }

        Instant t0 = Instant.now();
        RecomputeEvent saved = repository.save(event);
        log.info("RecomputeEventService.append(planId={}, reason={}) -> saved event id={} in {}",
                event.getPlanId(), event.getReason(), saved.getId(), Duration.between(t0, Instant.now()));
        return saved;
    }

    @Override
    public List<RecomputeEvent> findByPlanId(UUID planId) {
        log.debug("RecomputeEventService.findByPlanId(planId={})", planId);
        if (planId == null) {
            log.warn("RecomputeEventService.findByPlanId -> planId is null");
            throw new BusinessException(ErrorCode.MISSING_PARAMETER, "planId is required");
        }
        Instant t0 = Instant.now();
        List<RecomputeEvent> list = repository.findByPlanId(planId);
        log.info("RecomputeEventService.findByPlanId(planId={}) -> {} events in {}",
                planId, list.size(), Duration.between(t0, Instant.now()));
        return list;
    }
}
