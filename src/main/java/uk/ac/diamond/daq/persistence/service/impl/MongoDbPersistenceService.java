package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class MongoDbPersistenceService extends PersistenceServiceBase {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbPersistenceService.class);

    private static final String DB_NAME = "db-tests";
    private static final String ID_FIELD = "id";

    private MongoClient mongoClient;
    private MongoDatabase database;

    public void connect() {
        printMessage("Connecting...");
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase(DB_NAME);
    }

    public void disconnect() {
        printMessage("Disconnecting...");
        mongoClient.close();
    }

    @Override
    public void dropAll() {
        for (String collection : database.listCollectionNames()) {
            database.getCollection(collection).drop();
        }
    }

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        final String collectionName = getCollectionName(item);
        final String jsonString = toJson(item);
        final Document document = Document.parse(jsonString);

        // Remove default id and get a unique one from MongoDB
        if (document.get(ID_FIELD) == PersistableItem.INVALID_ID) {
            final ObjectId objectId = new ObjectId();
            document.put(ID_FIELD, objectId);

            final String idString = objectId.toString();
            final String idStringMongod = objectId.toStringMongod();
            final String idStringHex = objectId.toHexString();

            item.setId(new BigInteger("42"));
        }

        // Add class so we can reconstruct the object later
        document.append("class", item.getClass().getName());
        database.getCollection(collectionName).insertOne(document);
    }

    @Override
    public void delete(BigInteger persistenceId) {

    }

    @Override
    public void delete(PersistableItem item) {

    }

    private String toJson(PersistableItem item) throws PersistenceException {
        try {
            return new ObjectMapper().writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new PersistenceException("Error serialising object to JSON", e);
        }
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) throws PersistenceException {
        final String collectionName = getCollectionName(clazz);
        final SearchResult results = new SearchResult();
        final ObjectMapper objectMapper = new ObjectMapper();
        final FindIterable<Document> documents = database.getCollection(collectionName).find();

        for (Document document : documents) {
            // Copy document id to our id field
            final ObjectId id = document.getObjectId("_id");
            try {
                final String jsonString = document.toJson();
                final T item = objectMapper.readValue(jsonString, clazz);
                results.addResult(item);
            } catch (Exception e) {
                logger.error("Error adding document " + document, e);
            }
        }
        return results;
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz) throws PersistenceException {
        return null;
    }

    @Override
    public <T extends PersistableItem> T get(BigInteger persistenceId, Class<T> clazz) throws PersistenceException {
        return null;
    }

    @Override
    public List<Long> getVersions(BigInteger persistenceId) {
        return null;
    }

    @Override
    public <T extends PersistableItem> T getArchive(BigInteger persistenceId, long version, Class<T> clazz) throws PersistenceException {
        return null;
    }

    private String getCollectionName(Class<? extends PersistableItem> clazz) throws PersistenceException {
        // Find the last superclass before PersistableItem
        Class<?> collectionClass = clazz;
        while (!(collectionClass.getSuperclass().equals(PersistableItem.class))) {
            collectionClass = collectionClass.getSuperclass();
        }
        return collectionClass.getName();
    }

    private String getCollectionName(PersistableItem item) throws PersistenceException {
        return getCollectionName(item.getClass());
    }

    private void printMessage(String message) {
        System.out.println(message);
    }
}
