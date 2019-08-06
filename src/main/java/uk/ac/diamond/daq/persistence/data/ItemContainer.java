package uk.ac.diamond.daq.persistence.data;

import java.math.BigInteger;

public class ItemContainer {
    private String json;

    private BigInteger id;

    private long version;

    private Class<? extends PersistableItem> itemClass;

    public ItemContainer(PersistableItem item, String json) {
        this.id = item.getId();
        this.version = item.getVersion();
        this.json = json;
        this.itemClass = item.getClass();
    }

    public BigInteger getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getJson() {
        return json;
    }

    public Class<? extends PersistableItem> getItemClass() {
        return itemClass;
    }
}
