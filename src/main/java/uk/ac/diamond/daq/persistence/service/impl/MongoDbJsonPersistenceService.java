package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.VisitService;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class MongoDbJsonPersistenceService extends AbstractPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbJsonPersistenceService.class);

    private static final String ACTIVE_COLLECTION_NAME = "active";
    private static final String ARCHIVE_COLLECTION_NAME = "archive";
    private static final String CLASSES = "classes";
    private static final String PERSISTENCE_ID = "id";
    private static final String DATABASE_ID = "_id";
    private static final String VERSION = "version";

    private final Random rng = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MongoDatabase database;

    private final JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();

    public MongoDbJsonPersistenceService(MongoDatabase database, JsonSerialisationFactory jsonSerialisationFactory, VisitService visitService) {
        super(jsonSerialisationFactory, visitService);
        this.database = database;
    }

    @Override
    public long getNextPersistenceId() {
        // TODO: replace with incrementing number
        return Math.abs(rng.nextLong());
    }

    @Override
    protected ItemContainer getActive(long persistenceId, String visitId) {
        final Document searchDoc = new Document();
        searchDoc.put(PERSISTENCE_ID, persistenceId);
        final FindIterable<Document> dbResults = database.getCollection(getActiveCollectionName(visitId)).find(searchDoc);
        final Document document = dbResults.first();
        return (document == null) ? null : convertDocumentToItemContainer(document, visitId);
    }

    @Override
    protected void saveToActiveItems(ItemContainer itemContainer) {
        saveToCollection(itemContainer, getActiveCollectionName(itemContainer.getVisitId()));
    }

    @Override
    protected void saveToArchiveItems(ItemContainer itemContainer) {
        saveToCollection(itemContainer, getArchiveCollectionName(itemContainer.getVisitId()));
    }

    @Override
    protected <T extends PersistableItem> SearchResult get(Class<T> clazz, String visitId) throws PersistenceException {
        final Document searchDoc = new Document();
        searchDoc.put(CLASSES, clazz.getName());
        final FindIterable<Document> dbResult = database.getCollection(getActiveCollectionName(visitId)).find(searchDoc);
        return createSearchResult(dbResult);
    }

    @Override
    protected <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz, String visitId) throws PersistenceException {
        final Document searchDoc = new Document();
        for (Map.Entry<String, String> param : searchParameters.entrySet()) {
            final String key = param.getKey();
            final String value = param.getValue();
            if (key.equals(PERSISTENCE_ID) || key.equals(VERSION)) {
                searchDoc.put(PERSISTENCE_ID, Long.valueOf(value));
            } else {
                searchDoc.put(key, value);
            }
        }
        searchDoc.put(CLASSES, clazz.getName());

        final FindIterable<Document> activeResults = database.getCollection(getActiveCollectionName(visitId)).find(searchDoc);
        final FindIterable<Document> archiveResults = database.getCollection(getArchiveCollectionName(visitId)).find(searchDoc);
        return createSearchResult(activeResults, archiveResults);
    }

    @Override
    public List<Long> getVersions(long persistenceId, String visitId) {
        return null;
    }

    @Override
    protected ItemContainer getArchivedItem(long persistenceId, long version, String visitId) {
        final Document searchDoc = new Document();
        searchDoc.put(PERSISTENCE_ID, persistenceId);
        searchDoc.put(VERSION, version);
        final FindIterable<Document> dbResult = database.getCollection(getActiveCollectionName(visitId)).find(searchDoc);
        final Document document = dbResult.first();
        return document == null ? null : convertDocumentToItemContainer(document, visitId);
    }

    @Override
    public boolean delete(long persistenceId) {
        for (String collection : database.listCollectionNames()) {
            if (deleteFromCollection(collection, persistenceId)) {
                return true;
            }
        }
        return false;
    }

    private String getActiveCollectionName(String visitId) {
        return ACTIVE_COLLECTION_NAME + "_" + visitId;
    }

    private String getArchiveCollectionName(String visitId) {
        return ARCHIVE_COLLECTION_NAME + "_" + visitId;
    }

    private void saveToCollection(ItemContainer itemContainer, String collection) {
        final String json = itemContainer.getJson();
        final Document doc = Document.parse(json);
        doc.put(CLASSES, itemContainer.getClassNames());
        database.getCollection(collection).insertOne(doc);
    }

    private ItemContainer convertDocumentToItemContainer(Document document, String visitId) {
        // Save class name: remove this and database id
        final String className = ((List<String>) document.get(CLASSES)).get(0);
        document.remove(CLASSES);
        document.remove(DATABASE_ID);

        final String jsonString = document.toJson(jsonWriterSettings);
        final PersistableItem persistableItem = convertToPersistableItem(jsonString, className);
        return persistableItem == null ? null : new ItemContainer(persistableItem, jsonString, visitId);
    }

    private PersistableItem convertDocumentToPersistableItem(Document document) {
        // Save class name: remove it and database id
        final String className = ((List<String>) document.get(CLASSES)).get(0);
        document.remove(CLASSES);
        document.remove(DATABASE_ID);

        return convertToPersistableItem(document.toJson(jsonWriterSettings), className);
    }

    private PersistableItem convertToPersistableItem(String jsonString, String className) {
        try {
            final Class<? extends PersistableItem> clazz = (Class<? extends PersistableItem>) Class.forName(className);
            return objectMapper.readValue(jsonString, clazz);
        } catch (Exception e) {
            logger.error("Error deserialising document", e);
        }
        return null;
    }

    private SearchResult createSearchResult(FindIterable<Document>... docIterables) throws PersistenceException {
        final SearchResult searchResult = new SearchResult();
        for (FindIterable<Document> documents : docIterables) {
            for (Document doc : documents) {
                searchResult.addResult(convertDocumentToPersistableItem(doc));
            }
        }
        return searchResult;
    }

    private boolean deleteFromCollection(String collection, long persistenceId) {
        final Document deleteRequest = new Document();
        deleteRequest.put(PERSISTENCE_ID, Long.toString(persistenceId));
        final DeleteResult deleteResult = database.getCollection(collection).deleteMany(deleteRequest);

        return deleteResult.getDeletedCount() > 0;
    }

    public void dropAll() {
        for (String collection : database.listCollectionNames()) {
            database.getCollection(collection).drop();
        }
    }
}
