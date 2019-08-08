package uk.ac.diamond.daq.persistence.logging;

import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.InMemoryConfiguration;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

public class InMemoryConfigurationLogServiceTest extends ConfigurationLogServiceTest {
    @Before
    public void setup() {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);

        configurationLogService = applicationContext.getBean("configurationLogService", ConfigurationLogService.class);
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);
    }
}
