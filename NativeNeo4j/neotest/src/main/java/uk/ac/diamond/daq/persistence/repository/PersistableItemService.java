package uk.ac.diamond.daq.persistence.repository;

import uk.ac.diamond.daq.persistence.domain.PersistableItem;

public class PersistableItemService extends GenericService<PersistableItem> {

    @Override
    Class getEntityType() {
        return PersistableItem.class;

    }

}
