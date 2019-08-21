package uk.ac.diamond.daq.persistence.service.neonative;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.SearchResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public interface Neo4jUtil {

    default String generateCypherString(HashMap<String, Object> searchParameters) {
        String baseString = "MATCH (n)";
        if (searchParameters.size() > 0) {
            baseString.concat(" WHERE ");
        }
        Iterator keyIterator = searchParameters.entrySet().iterator();
        while (keyIterator.hasNext()) {
            Map.Entry kvPair = (Map.Entry) keyIterator.next();
            baseString.concat(" n." + kvPair.getKey());
            if (!kvPair.getValue().toString().contains("CONTAINS")) {
                baseString.concat(" = $" + kvPair.getKey());
            } else {
                baseString.concat((String) kvPair.getValue());
                kvPair.setValue(kvPair.getValue().toString().split(" ")[1]);
            }
            if (keyIterator.hasNext()) {
                baseString.concat(" AND");
            }
        }
        return baseString;
    }

    default String generateCypherString(HashMap<String, Object> query, String suffixQualifier) {
        return generateCypherString(query) + suffixQualifier;
    }

    default <T extends PersistableItem> SearchResult formatSearchResults(Iterable<T> collect) throws PersistenceException {
        SearchResult results = new SearchResult();
        for (T item : collect) {
            results.addResult(item);
        }
        return results;
    }
}
