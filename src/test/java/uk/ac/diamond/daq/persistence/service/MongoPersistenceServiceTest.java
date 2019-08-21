package uk.ac.diamond.daq.persistence.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.After;
import org.junit.Before;
import uk.ac.diamond.daq.persistence.json.impl.DefaultJsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.impl.MongoDbJsonPersistenceService;
import uk.ac.diamond.daq.persistence.service.impl.TestVisitService;

public class MongoPersistenceServiceTest extends PersistenceServiceTest {
    private static final String DB_NAME = "db-test";

    private MongoClient mongoClient;

    @Before
    public void setUp() throws PersistenceException {
        mongoClient = MongoClients.create();
        final MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        persistenceService = new MongoDbJsonPersistenceService(database, new DefaultJsonSerialisationFactory(), new TestVisitService("current"));

        createTestData();
    }

    @After
    public void tearDown() {
        ((MongoDbJsonPersistenceService) persistenceService).dropAll();
        mongoClient.close();
    }
}
