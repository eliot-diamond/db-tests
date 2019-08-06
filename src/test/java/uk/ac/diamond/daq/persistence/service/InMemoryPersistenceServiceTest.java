package uk.ac.diamond.daq.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.InMemoryConfiguration;

public class InMemoryPersistenceServiceTest extends PersistenceServiceTest {
    @Override
    @Before
    public void setup() throws PersistenceException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(InMemoryConfiguration.class);
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);

        super.setup();
    }

    @Override
    @After
    public void tearDown() {

        super.tearDown();
    }
}
