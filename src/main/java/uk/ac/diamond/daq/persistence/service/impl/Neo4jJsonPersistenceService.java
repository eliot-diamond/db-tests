package uk.ac.diamond.daq.persistence.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.service.Neo4jSessionFactory;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;
import uk.ac.diamond.daq.persistence.service.VisitService;

import java.util.*;

public class Neo4jJsonPersistenceService extends AbstractPersistenceService implements Neo4jUtil {

    private static final Logger log = LoggerFactory.getLogger(Neo4jJsonPersistenceService.class);

    private static Long idGen = 512l;

    public Neo4jJsonPersistenceService(JsonSerialisationFactory jsonSerialisationFactory, VisitService visitService) {
        super(jsonSerialisationFactory, visitService);
    }

    private static final int DEPTH_LIST = 0;
    private static final int DEPTH_ENTITY = 1;

    protected Session session = Neo4jSessionFactory.getInstance().getNeo4jSession();

    @Override
    protected <T extends PersistableItem> SearchResult get(Class<T> clazz, String visitId) throws PersistenceException {
        Map<String, String> query = new HashMap<>();
        return get(query, clazz, visitId);
    }

    @Override
    public boolean delete(long persistenceId) {
        PersistableItem itemToDelete = session.load(PersistableItem.class, persistenceId);
        if (!itemToDelete.equals(null)) {
            session.delete(itemToDelete);
            return true;
        }
        return false;
    }

    @Override
    protected <T extends PersistableItem> SearchResult get(Map<String, String> searchParameters, Class<T> clazz, String visitId) throws PersistenceException {
        HashMap<String, Object> castMap = new HashMap<>();
        castMap.put("visitId", visitId);
        castMap.put("classes", " CONTAINS " + clazz.getSimpleName());
        Iterable<ItemContainer> serialisedResults = session.query(ItemContainer.class, generateCypherString(castMap, " RETURN n"), searchParameters);
        List<T> deserialisedResults = new ArrayList<>();
        for (ItemContainer contained : serialisedResults) {
            deserialisedResults.add(this.jsonSerialisationFactory.getJsonDeserialiser(this, visitId).deserialise(contained));
        }
        return formatSearchResults(deserialisedResults);
    }

    @Override
    public List<Long> getVersions(long persistenceId, String visitId) {
        return getVersions(persistenceId, visitId, session);
    }

    @Override
    protected ItemContainer getArchivedItem(long persistenceId, long version, String visitId) {
        HashMap<String, Object> query = new HashMap<>();
        query.put("persistenceId", persistenceId);
        query.put("version", version);
        query.put("visitId", visitId);

        return session.queryForObject(ItemContainer.class, generateCypherString(query, " RETURN n"), query);
    }

    @Override
    protected long getNextPersistenceId() {
        return ++idGen;
    }

    protected long getHighestVersion(long persistenceId) {
        HashMap<String, Object> query = new HashMap<>();
        query.put("persistenceId", persistenceId);
        try {
            return session.queryForObject(Long.class, generateCypherString(query, " RETURN max(n.version) LIMIT 1"), query);
        } catch (NullPointerException e) {
            return 1;
        }
    }

    @Override
    protected ItemContainer getActive(long persistenceId, String visitId) {
//        HashMap<String, Object> query = new HashMap<>();
//        query.put("persistenceId", persistenceId);
//        query.put("visitId", visitId);
//        String qualifier = " RETURN n ORDER BY n.version LIMIT 1";
//        ItemContainer toReturn = session.queryForObject(ItemContainer.class, generateCypherString(query, qualifier), query);
//        System.out.print(toReturn.toString());
        return null;
    }

    @Override
    protected void saveToActiveItems(ItemContainer itemContainer) {

        itemContainer.setId(getNextPersistenceId());

        itemContainer.setId(getHighestVersion(itemContainer.getId()) + 1);
        saveToArchiveItems(itemContainer);

        ObjectMapper objectMapper;
        ObjectNode objectNode;
        for (Map.Entry<String, JsonNode> entry ; objectNode.findValues()) {
            if (node instanceof ObjectNode) {
                String value = objectMapper.readValue(node);
                //store value
            } else if (node instanceof ArrayNode) {
                //do arrary node type things
            } else {
                //store key value pair in neo node
            }
        }

    }

    @Override
    protected void saveToArchiveItems(ItemContainer itemContainer) {
        session.save(itemContainer);
    }

    public void tearDown() {
        session.purgeDatabase();
    }

}