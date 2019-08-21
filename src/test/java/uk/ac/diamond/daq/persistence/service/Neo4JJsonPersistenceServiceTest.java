package uk.ac.diamond.daq.persistence.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.diamond.daq.persistence.configuration.Neo4JJsonConfiguration;

public class Neo4JJsonPersistenceServiceTest extends PersistenceServiceTest{

    @Before
    public void setUp() throws PersistenceException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(Neo4JJsonConfiguration.class);
        persistenceService = applicationContext.getBean("persistenceService", PersistenceService.class);

        createTestData();
    }

    @Override
    @Test(expected = IllegalArgumentException.class)
    public void deleteDoesntRetrieve() throws PersistenceException {
        super.deleteDoesntRetrieve();
    }

    @Override
    @Test(expected = IllegalArgumentException.class)
    public void deleteUsingID() throws PersistenceException {
        super.deleteUsingID();

    }

    @Override
    @Test(expected = IllegalArgumentException.class)
    public void deleteExceptsMapRetrieval() throws PersistenceException {
        super.deleteExceptsMapRetrieval();
    }

    @Override
    @Test(expected = IllegalArgumentException.class)
    public void deleteExceptsContainerRetrieval() throws PersistenceException {
        super.deleteExceptsContainerRetrieval();
    }

//    @After
//    public void tearDown() {
//        ((Neo4jJsonPersistenceService) persistenceService).tearDown();
//    }
}
