package uk.ac.diamond.daq.peristence.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.diamond.daq.peristence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.peristence.logging.impl.InMemoryConfigurationLogService;
import uk.ac.diamond.daq.peristence.service.ExecutionService;
import uk.ac.diamond.daq.peristence.service.PersistenceService;
import uk.ac.diamond.daq.peristence.service.impl.InMemoryPersistenceService;

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
