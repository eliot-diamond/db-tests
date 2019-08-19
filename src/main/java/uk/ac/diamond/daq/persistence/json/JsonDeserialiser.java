package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.util.List;

public interface JsonDeserialiser {
    <T extends PersistableItem> T deserialise(ItemContainer itemContainer) throws PersistenceException;

    List<PersistableItem> getCache();
}
