package uk.ac.diamond.daq.persistence.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.diamond.daq.persistence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.persistence.logging.impl.InMemoryConfigurationLogService;
import uk.ac.diamond.daq.persistence.service.ExecutionService;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.impl.InMemoryPersistenceService;

@Configuration
public class InMemoryConfiguration {
    @Bean
    PersistenceService persistenceService() {
        return new InMemoryPersistenceService();
    }

    @Bean
    ConfigurationLogService configurationLogService(PersistenceService persistenceService) {
        return new InMemoryConfigurationLogService(persistenceService);
    }

    @Bean
    ExecutionService executionService(ConfigurationLogService configurationLogService) {
        return new ExecutionService(configurationLogService);
    }
}
