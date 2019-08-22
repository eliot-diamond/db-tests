package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

public interface JsonSerialiser {
    String serialise(Persistable item) throws PersistenceException;
}
