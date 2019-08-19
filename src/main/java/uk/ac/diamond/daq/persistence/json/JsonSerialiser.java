package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

public interface JsonSerialiser {
    String serialise(PersistableItem item) throws PersistenceException;
}
