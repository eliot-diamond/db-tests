package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.*;
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

public class MongoDbJsonPersistenceService extends JsonPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbJsonPersistenceService.class);

    private static final String DB_NAME = "db-tests";
    private static final String POJO_ID = "id";     // id field in our objects
    private static final String MONGO_ID = "_id";   // id field in database
    private static final String CLASS = "class";

    private static final int HEX_RADIX = 16;

    private MongoClient mongoClient;
    private MongoDatabase database;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void connect() {
        printMessage("Connecting...");
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase(DB_NAME);
    }

    public void disconnect() {
        printMessage("Disconnecting...");
        mongoClient.close();
    }

    public void dropAll() {
        for (String collection : database.listCollectionNames()) {
            database.getCollection(collection).drop();
        }
    }

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        final String jsonString = toJson(item);
        final Document document = Document.parse(jsonString);
        final BigInteger docId = getDocumentId(document);

        if (docId.equals(PersistableItem.INVALID_ID)) {
            // No id has been set for this object, so get a new one from MongoDB
            final ObjectId objectId = new ObjectId();
            document.put(MONGO_ID, objectId);
            item.setId(new BigInteger(objectId.toHexString(), HEX_RADIX));
        } else {
            document.put(MONGO_ID, bigIntegerToObjectId(item.getId()));
        }

        // Remove our id from the document because we have set the Mongo id
        document.remove(POJO_ID);
        // Add class so we can reconstruct the object later
        document.append(CLASS, item.getClass().getName());
        getCollection(item).insertOne(document);
    }

    @Override
    public void delete(BigInteger persistenceId) {

    }

    @Override
    public void delete(PersistableItem item) {

    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) throws PersistenceException {
        return convertDocumentsToSearchResult(getCollection(clazz).find());
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz) throws PersistenceException {
        final Document searchDoc = new Document();
        for (Map.Entry<String, String> searchParam : searchParameters.entrySet()) {
            searchDoc.put(searchParam.getKey(), searchParam.getValue());
        }
        searchDoc.put(CLASS, clazz.getName());
        return convertDocumentsToSearchResult(getCollection(clazz).find(searchDoc));
    }

    @Override
    public <T extends PersistableItem> T get(BigInteger persistenceId, Class<T> clazz) throws PersistenceException {
        final Document searchDoc = new Document();
        searchDoc.put(MONGO_ID, bigIntegerToObjectId(persistenceId));
        searchDoc.put(CLASS, clazz.getName());

        final FindIterable<Document> documents = getCollection(clazz).find(searchDoc);
        final Document result = documents.first();
        if (result == null) {
            throw new PersistenceException("No result found for id " + persistenceId.toString());
        }
        return convertDocumentToObject(result);
    }

    @Override
    public List<Long> getVersions(BigInteger persistenceId) {
        return null;
    }

    @Override
    public <T extends PersistableItem> T getArchive(BigInteger persistenceId, long version, Class<T> clazz) throws PersistenceException {
        return null;
    }

    /**
     * Get the collection corresponding to a class<br>
     * If will be the last superclass before PersistableItem
     *
     * @param clazz The class whose collection is to be found
     * @return The corresponding collection
     * @throws PersistenceException
     */
    private MongoCollection getCollection(Class<? extends PersistableItem> clazz) throws PersistenceException {
        Class<?> collectionClass = clazz;
        while (!(collectionClass.getSuperclass().equals(PersistableItem.class))) {
            collectionClass = collectionClass.getSuperclass();
        }
        return database.getCollection(collectionClass.getName());
    }

    /**
     * Get the collection corresponding to a {@link PersistableItem}
     *
     * @param item The item whose collection is to be found
     * @return The corresponding collection
     * @throws PersistenceException
     */
    private MongoCollection getCollection(PersistableItem item) throws PersistenceException {
        return getCollection(item.getClass());
    }

    private String toJson(PersistableItem item) throws PersistenceException {
        try {
            return new ObjectMapper().writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new PersistenceException("Error serialising object to JSON", e);
        }
    }

    /**
     * Return id field in document as BigInteger
     *
     * @return id as BigInteger
     */
    private BigInteger getDocumentId(Document document) throws PersistenceException {
        final Object docId = document.get(POJO_ID);
        if (docId instanceof Integer) {
            return BigInteger.valueOf(((Integer) docId).longValue());
        } else if (docId instanceof Long) {
            return BigInteger.valueOf(((Long) docId).longValue());
        } else if (docId instanceof BigInteger) {
            return (BigInteger) docId;
        } else if (docId instanceof ObjectId) {
            final ObjectId objId = ((ObjectId) docId);
            return objectIdToBigInteger(((ObjectId) docId));
        } else {
            throw new PersistenceException("Invalid document id " + docId);
        }
    }

    private <T extends PersistableItem> T convertDocumentToObject(Document document) throws PersistenceException {
        // Save class name and remove from document, as it does not appear in the object
        final String className = (String) document.get(CLASS);
        document.remove(CLASS);

        // Move MongoDBs object id to GDAs id field
        final ObjectId id = document.getObjectId(MONGO_ID);
        document.put(POJO_ID, id.toHexString());
        document.remove(MONGO_ID);
        final String jsonString = document.toJson();

        // Find class of object we want to create
        try {
            final Class objectClass = Class.forName(className);
            return (T) objectMapper.readValue(jsonString, objectClass);
        } catch (Exception e) {
            throw new PersistenceException("Error converting document " + document, e);
        }
    }

    private SearchResult convertDocumentsToSearchResult(FindIterable<Document> documents) {
        final SearchResult results = new SearchResult();
        for (Document document : documents) {
            try {
                results.addResult(convertDocumentToObject(document));
            } catch (Exception e) {
                logger.error("Error adding document " + document, e);
            }
        }
        return results;
    }

    private BigInteger objectIdToBigInteger(ObjectId objectId) {
        return new BigInteger(objectId.toHexString(), HEX_RADIX);
    }

    private ObjectId bigIntegerToObjectId(BigInteger bigInt) {
        return new ObjectId(bigInt.toString(HEX_RADIX));
    }

    private void printMessage(String message) {
        System.out.println(message);
    }
}
