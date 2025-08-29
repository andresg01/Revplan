package com.revplan.llm.application.service.impl;

import com.revplan.llm.domain.model.HealthLLM;
import com.revplan.llm.domain.repository.HealthRepository;
import com.revplan.llm.domain.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HealthRepository healthRepository;

    @Override
    public HealthLLM check() {
        return healthRepository.check();
    }
}
