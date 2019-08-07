package uk.ac.diamond.daq.persistence.service;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.InMemoryConfiguration;

public class InMemoryPersistenceServiceTest extends PersistenceServiceTest {

    @Override
    public void beforeSetUp() {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);
    }
}
