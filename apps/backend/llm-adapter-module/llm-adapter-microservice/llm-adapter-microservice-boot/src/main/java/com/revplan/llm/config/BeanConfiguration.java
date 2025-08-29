
package com.revplan.llm.config;

import com.revplan.llm.application.service.impl.HealthServiceImpl;
import com.revplan.llm.domain.repository.HealthRepository;
import com.revplan.llm.domain.service.HealthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.revplan.llm.Application;


/**
 * Class to configure Domain beans to use
 *
 * @author agarciab
 *
 */
@Configuration
@ComponentScan(basePackageClasses = Application.class)
public class BeanConfiguration {

    @Bean
    HealthService healthService(final HealthRepository healthRepository) {
        return new HealthServiceImpl(healthRepository);
    }
}
