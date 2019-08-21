package uk.ac.diamond.daq.persistence.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.diamond.daq.persistence.json.impl.DefaultJsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceService;
import uk.ac.diamond.daq.persistence.service.VisitService;
import uk.ac.diamond.daq.persistence.service.impl.Neo4jNativePersistenceService;
import uk.ac.diamond.daq.persistence.service.impl.TestVisitService;

@Configuration
public class Neo4JNativeConfiguration {

    @Bean
    VisitService visitService() {
        return new TestVisitService("current");
    }

    @Bean
    PersistenceService persistenceService(VisitService visitService) {
        return new Neo4jNativePersistenceService(new DefaultJsonSerialisationFactory(), visitService);
    }

//    @Bean
//    ConfigurationLogService configurationLogService(PersistenceService persistenceService) {
//        return new InMemoryConfigurationLogService(persistenceService);
//    }
}
