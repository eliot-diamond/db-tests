package uk.ac.diamond.daq.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import uk.ac.diamond.daq.persistence.json.impl.DefaultJsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.impl.Neo4jNativePersistenceService;
import uk.ac.diamond.daq.persistence.service.impl.TestVisitService;

public class Neo4jNativePersistenceServiceTest extends PersistenceServiceTest {

    private static ApplicationContext applicationContext;

    @Before
    public void setUp() throws PersistenceException {
//        applicationContext = new AnnotationConfigApplicationContext(Neo4JNativeConfiguration.class);
//
//        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);

        persistenceService = new Neo4jNativePersistenceService(new DefaultJsonSerialisationFactory(), new TestVisitService("currentVisit"));

        createTestData();
    }

    @After
    public void tearDown() {
        ((Neo4jNativePersistenceService) persistenceService).tearDown();
    }

}
