package uk.ac.diamond.daq.persistence.service;

import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.Neo4JNativeConfiguration;

public class Neo4jNativePersistenceServiceTest extends PersistenceServiceTest {

    private static ApplicationContext applicationContext;

    @BeforeClass
    public static void initialise() {
        applicationContext = new AnnotationConfigApplicationContext(Neo4JNativeConfiguration.class);
    }

    @Before
    public void setUp() throws PersistenceException {
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);

        createTestData();
    }

//    @After
//    public void tearDown() {
//        ((Neo4jNativePersistenceService) persistenceService).tearDown();
//    }


}
