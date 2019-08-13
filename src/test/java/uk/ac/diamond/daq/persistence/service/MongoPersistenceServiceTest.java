package uk.ac.diamond.daq.persistence.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.Before;
import uk.ac.diamond.daq.persistence.service.impl.MongoDbJsonPersistenceService;

public class MongoPersistenceServiceTest extends PersistenceServiceTest {
    private static final String DB_NAME = "db-test";

    private MongoClient mongoClient;
    private MongoDatabase database;

    @Before
    public void setUp() throws PersistenceException {
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase(DB_NAME);
        persistenceService = null;//new MongoDbJsonPersistenceService(database);

        createTestData();
    }

    @After
    public void tearDown() {
        ((MongoDbJsonPersistenceService) persistenceService).dropAll();
        mongoClient.close();
    }
}
