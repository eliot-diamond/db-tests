package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.Persistable;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.util.List;

public interface JsonDeserialiser {
    <T extends Persistable> T deserialise(ItemContainer itemContainer) throws PersistenceException;

    List<Persistable> getCache();
}
