package uk.ac.diamond.daq.persistence.service.impl;

import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.PersistableItem;

public class ItemContainer extends ItemReference {
    private String json;

    public ItemContainer(PersistableItem item, String json) {
        super(item.getId(), item.getVersion(), item.getClass());
        this.json = json;
    }

    public String getJson() {
        return json;
    }
}
