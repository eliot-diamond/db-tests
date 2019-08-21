package uk.ac.diamond.daq.persistence.service.neonative;

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
        return session.query(getEntityType(), label(generateCypherString(searchParameters), labels), searchParameters);
    }

    @Override
    public PersistableItem getOneForLabels(ArrayList<String> labels, HashMap<String, Object> searchParameters) {
        return session.queryForObject(getEntityType(), label(generateCypherString(searchParameters), labels), searchParameters);
    }

    private String label(String unlabelled, ArrayList<String> labels){
        String labelled = "(n";
        for (String label : labels){
            labelled = labelled.concat(":"+label);
        }
        labelled = labelled.concat(")");
        return unlabelled.replace("(n)", labelled);
    }
}
