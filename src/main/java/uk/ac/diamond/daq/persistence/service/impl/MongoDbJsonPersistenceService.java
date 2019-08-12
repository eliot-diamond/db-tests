package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;

public class MongoDbJsonPersistenceService extends JsonPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbJsonPersistenceService.class);

    private static final String POJO_ID = "id";     // id field in our objects
    private static final String MONGO_ID = "_id";   // id field in database
    private static final String CLASSES = "classes";

    // Regexs for converting id in Java format to Mongo format
    private static final String JAVA_TO_MONGO_PATTERN = "\"id\"[ ]*:[ ]*\"([0-9a-fA-F]{24})\"";
    private static final String JAVA_TO_MONGO_REPLACEMENT = "\"_id\" : \\{\"\\$oid\" : \"$1\"\\}";

    private static final int HEX_RADIX = 16;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MongoDatabase database;

    public MongoDbJsonPersistenceService(MongoDatabase database) {
        this.database = database;
    }

    public void dropAll() {
        for (String collection : database.listCollectionNames()) {
            database.getCollection(collection).drop();
        }
    }

    @Override
    public void save(PersistableItem item) throws PersistenceException {
        setIds(item);
        final String jsonString = serialize(item);
        final String jsonWithMongoId = jsonString.replaceAll(JAVA_TO_MONGO_PATTERN, JAVA_TO_MONGO_REPLACEMENT);
        final Document document = Document.parse(jsonWithMongoId);

        // Add class hierarchy so we can search for subclasses and reconstruct the object later
        final List<String> classes = new ArrayList<>();
        Class<?> objectClass = item.getClass();
        while (!objectClass.equals(Object.class)) {
            classes.add(objectClass.getName());
            objectClass = objectClass.getSuperclass();
        }
        document.append(CLASSES, classes);
        getCollection(item).insertOne(document);
    }

    /**
     * Recursively set ids in a {@link PersistableItem} that are not yet set
     *
     * @param item The {@link PersistableItem} to process
     */
    private void setIds(PersistableItem item) throws PersistenceException {
        if (item == null) {
            return;
        }
        // Process the top-level id
        if (item.getId().equals(PersistableItem.INVALID_ID)) {
            final ObjectId objectId = new ObjectId();
            item.setId(new BigInteger(objectId.toHexString(), HEX_RADIX));
        }
        // Loop over fields
        final Class<? extends PersistableItem> classObject = item.getClass();
        final Field[] fields = classObject.getFields();
        for (Field field : fields) {
            try {
                final Class<?> fieldClass = field.getType();
                if (Collection.class.isAssignableFrom(fieldClass)) {
                    setIds((Collection) field.get(item));
                } else if (PersistableItem.class.isAssignableFrom(fieldClass)) {
                    setIds((PersistableItem) field.get(item));
                }
            } catch (Exception e) {
                throw new PersistenceException("Error setting item id", e);
            }
        }
    }

    /**
     * Recursive over a {@link Collection} setting ids in any {@link PersistableItem}s found
     *
     * @param itemCollection The {@link Collection} to process
     */
    private void setIds(Collection<?> itemCollection) throws PersistenceException {
        if (itemCollection == null) {
            return;
        }
        for (Object item : itemCollection) {
            final Class itemClass = item.getClass();
            if (Collection.class.isAssignableFrom(itemClass)) {
                setIds((Collection) item);
            } else if (PersistableItem.class.isAssignableFrom(itemClass)) {
                setIds((PersistableItem) item);
            }
        }
    }

    @Override
    public void delete(BigInteger persistenceId) {

    }

    @Override
    public void delete(PersistableItem item) {

    }

    private <T extends PersistableItem> FindIterable<Document> searchForDocuments(Map<String, ? extends Object> searchParameters, Class<T> clazz) {
        final Document searchDoc = new Document();
        for (Map.Entry<String, ? extends Object> searchParam : searchParameters.entrySet()) {
            searchDoc.put(searchParam.getKey(), searchParam.getValue());
        }
        // Search for the name of the requested class anywhere in the class hierarchy field
        searchDoc.put(CLASSES, clazz.getName());
        return getCollection(clazz).find(searchDoc);
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Class<T> clazz) {
        return get(Collections.emptyMap(), clazz);
    }

    @Override
    public <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz) {
        return convertDocumentsToSearchResult(searchForDocuments(searchParameters, clazz));
    }

    @Override
    public <T extends PersistableItem> T get(BigInteger persistenceId, Class<T> clazz) throws PersistenceException {
        final Map<String, Object> idParam = ImmutableMap.of(MONGO_ID, bigIntegerToObjectId(persistenceId));
        final FindIterable<Document> documents = searchForDocuments(idParam, clazz);
        return convertDocumentToObject(documents.first());
    }

    @Override
    public List<Long> getVersions(BigInteger persistenceId) {
        return null;
    }

    @Override
    public <T extends PersistableItem> T getArchive(BigInteger persistenceId, long version, Class<T> clazz) {
        return null;
    }

    /**
     * Get the collection corresponding to a class<br>
     * If will be the last superclass before PersistableItem
     *
     * @param clazz The class whose collection is to be found
     * @return The corresponding collection
     */
    private MongoCollection<Document> getCollection(Class<? extends PersistableItem> clazz) {
        Class<?> collectionClass = clazz;
        while (!(collectionClass.getSuperclass().equals(PersistableItem.class))) {
            collectionClass = collectionClass.getSuperclass();
        }
        return database.getCollection(collectionClass.getSimpleName());
    }

    /**
     * Get the collection corresponding to a {@link PersistableItem}
     *
     * @param item The item whose collection is to be found
     * @return The corresponding collection
     */
    private MongoCollection<Document> getCollection(PersistableItem item) {
        return getCollection(item.getClass());
    }

    /**
     * Return id field in document as BigInteger
     *
     * @return id as BigInteger
     */
    private BigInteger getDocumentId(Document document) throws PersistenceException {
        final Object docId = document.get(POJO_ID);
        if (docId instanceof Integer) {
            return BigInteger.valueOf((Integer) docId);
        } else if (docId instanceof Long) {
            return BigInteger.valueOf((Long) docId);
        } else if (docId instanceof BigInteger) {
            return (BigInteger) docId;
        } else if (docId instanceof String) {
            return new BigInteger((String) docId);
        } else if (docId instanceof ObjectId) {
            return objectIdToBigInteger((ObjectId) docId);
        } else {
            throw new PersistenceException("Invalid document id " + docId);
        }
    }

    private <T extends PersistableItem> T convertDocumentToObject(Document document) throws PersistenceException {
        // Save class name and remove from document, as it does not appear in the object
        final String className = ((List<String>) document.get(CLASSES)).get(0);
        document.remove(CLASSES);

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
        final int idLength = 24;
        String bigIntAsHex = bigInt.toString(HEX_RADIX);
        final int padLength = idLength - bigIntAsHex.length();
        if (padLength > 1) {
            final StringBuilder paddedValue = new StringBuilder();
            for (int i = 0; i < padLength; i++) {
                paddedValue.append('0');
            }
            paddedValue.append(bigIntAsHex);
            bigIntAsHex = paddedValue.toString();
        }
        return new ObjectId(bigIntAsHex);
    }
}
