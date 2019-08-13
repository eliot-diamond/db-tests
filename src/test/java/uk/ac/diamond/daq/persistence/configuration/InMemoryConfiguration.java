package uk.ac.diamond.daq.persistence.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.diamond.daq.persistence.json.impl.DefaultJsonSerializer;
import uk.ac.diamond.daq.persistence.logging.ConfigurationLogService;
import uk.ac.diamond.daq.persistence.logging.impl.InMemoryConfigurationLogService;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.impl.InMemoryJsonPersistenceService;

@Configuration
public class InMemoryConfiguration {
    @Bean
    PersistenceService persistenceService() {
        DefaultJsonSerializer defaultJsonSerializer = new DefaultJsonSerializer();
        return new InMemoryJsonPersistenceService(defaultJsonSerializer);
    }

    @Bean
    ConfigurationLogService configurationLogService(PersistenceService persistenceService) {
        return new InMemoryConfigurationLogService(persistenceService);
    }
}
