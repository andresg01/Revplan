package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.model.Limits;
import com.revplan.llm.domain.repository.LimitsRepository;
import com.revplan.llm.domain.service.LimitsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LimitsServiceImpl implements LimitsService {

    private final LimitsRepository limitsRepository;

    @Override
    public Limits getLimits() {
        return limitsRepository.fetchLimits();
    }
}
