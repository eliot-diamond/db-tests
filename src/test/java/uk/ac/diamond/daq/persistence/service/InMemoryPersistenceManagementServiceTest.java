package uk.ac.diamond.daq.persistence.service;

import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.InMemoryConfiguration;

public class InMemoryPersistenceManagementServiceTest extends PersistenceManagementServiceTest {
    @Before
    public void setup() {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);
        persistenceManagementService = applicationContext.getBean("persistenceService", PersistenceManagementService.class);
        visitService = applicationContext.getBean("visitService", VisitService.class);
    }
}
