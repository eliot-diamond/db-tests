package uk.ac.diamond.daq.persistence.service.impl;

import org.neo4j.ogm.session.Session;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.util.*;

public interface Neo4jUtil {

    default String generateCypherString(HashMap<String, Object> searchParameters, ArrayList<String> labels) {
        String baseString = "MATCH (n";
        for (String label : labels){
            baseString = baseString.concat(":"+label);
        }
        baseString = baseString.concat(")");
        if (searchParameters.size() > 0) {
            baseString = baseString.concat(" WHERE ");
        }
        Iterator keyIterator = searchParameters.entrySet().iterator();
        while (keyIterator.hasNext()) {
            Map.Entry kvPair = (Map.Entry) keyIterator.next();
            if (kvPair.getKey().equals("persistenceId") || kvPair.getKey().equals("id")) {
                baseString = baseString.concat("ID(n)");
            } else {
                baseString = baseString.concat(" n." + kvPair.getKey());
            }
            if (!kvPair.getValue().toString().contains("CONTAINS")) {
                baseString = baseString.concat(" = $" + kvPair.getKey());
            } else {
                baseString = baseString.concat((String) kvPair.getValue());
                kvPair.setValue(kvPair.getValue().toString().split(" ")[1]);
            }
            if (keyIterator.hasNext()) {
                baseString = baseString.concat(" AND");
            }
        }
        return baseString.concat(" RETURN n");
    }

    default String generateCypherString(HashMap<String, Object> query, String suffixQualifier) {
        return generateCypherString(query, new ArrayList<String>()) + suffixQualifier;
    }

    default <T extends Persistable> SearchResult formatSearchResults(Iterable<T> collect) throws PersistenceException {
        SearchResult results = new SearchResult();
        for (T item : collect) {
            results.addResult((PersistableItem) item);
        }
        return results;
    }

    default List<Long> getVersions(long persistenceId, String visitId, Session session) {
        HashMap<String, Object> query = new HashMap<>();
        query.put("id", persistenceId);
        query.put("visitId", visitId);
        Iterator itemsGot = session.query(generateCypherString(query, " RETURN n"), query).iterator();
        ArrayList<Long> versionsGot = new ArrayList<>();
        while (itemsGot.hasNext()) {
            versionsGot.add(((ItemContainer) itemsGot.next()).getId());
        }
        return versionsGot;
    }
}
