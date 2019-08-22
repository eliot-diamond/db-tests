package uk.ac.diamond.daq.persistence.service.impl.neo4j;

import org.apache.commons.lang3.SerializationUtils;
import uk.ac.diamond.daq.persistence.data.PersistableItem;

import java.util.ArrayList;
import java.util.HashMap;

public class PersistableItemService extends GenericService<PersistableItem> {

    @Override
    public Class<PersistableItem> getEntityType() {
        return PersistableItem.class;
    }

    @Override
    public Iterable<PersistableItem> getForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters) {
        return session.query(getEntityType(), generateCypherString(searchParameters, labels), searchParameters);
    }

    @Override
    public PersistableItem getOneForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters) {
        return SerializationUtils.clone(session.queryForObject(getEntityType(), generateCypherString(searchParameters, labels), searchParameters));
    }
}
