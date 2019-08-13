package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.PersistenceService;

public interface JsonSerializer {
    <T extends PersistableItem> T deserialize(String json, Class<T> clazz, PersistenceService persistenceService)
            throws PersistenceException;

    String serialize(PersistableItem item, PersistenceService persistenceService) throws PersistenceException;
}
