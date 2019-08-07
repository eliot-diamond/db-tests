package uk.ac.diamond.daq.persistence.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import uk.ac.diamond.daq.persistence.service.impl.MongoDbJsonPersistenceService;

public class MongoPersistenceServiceTest extends PersistenceServiceTest {
    private static final String DB_NAME = "db-test";

    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public void beforeSetUp() {
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase(DB_NAME);
        persistenceService = new MongoDbJsonPersistenceService(database);
    }

    @Override
    public void afterTearDown() {
        ((MongoDbJsonPersistenceService) persistenceService).dropAll();
        mongoClient.close();
    }
}
