package com.revapp.planengine.config;

import com.revapp.planengine.application.service.impl.*;
import com.revapp.planengine.domain.repository.*;
import com.revapp.planengine.domain.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public PlanService planService(final PlanRepository planRepository,
                                   final PlanReadRepository planReadRepository) {
        return new PlanServiceImpl(planRepository, planReadRepository);
    }

    @Bean
    public PlanTemplateService planTemplateService(final PlanTemplateRepository planTemplateRepository) {
        return new PlanTemplateServiceImpl(planTemplateRepository);
    }

    @Bean
    public PlanGenerationService planGenerationService(final PlanRepository planRepository,
                                                       final PlanVersionRepository planVersionRepository,
                                                       final PlanReadRepository planReadRepository,
                                                       final PlanTemplateRepository planTemplateRepository) {
        return new PlanGenerationServiceImpl(
                planRepository,
                planVersionRepository,
                planReadRepository,
                planTemplateRepository
        );
    }

    @Bean
    public PlanSimulationService planSimulationService(final PlanVersionRepository planVersionRepository) {
        return new PlanSimulationServiceImpl(planVersionRepository);
    }

    @Bean
    public PlanRecomputeService planRecomputeService(final RecomputeEventRepository recomputeEventRepository,
                                                     final PlanRepository planRepository) {
        return new PlanRecomputeServiceImpl(recomputeEventRepository, planRepository);
    }

    @Bean
    public RecomputeEventService recomputeEventService(final RecomputeEventRepository recomputeEventRepository) {
        return new RecomputeEventServiceImpl(recomputeEventRepository);
    }

    @Bean
    public ScorePreviewService scorePreviewService(final PlanTemplateRepository templateRepository) {
        return new ScorePreviewServiceImpl(templateRepository);
    }
}
