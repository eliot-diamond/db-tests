package uk.ac.diamond.daq.persistence.data;

import java.math.BigInteger;

public class ItemReference {
    private BigInteger id;
    private long version;

    private Class<? extends PersistableItem> itemClass;

    public ItemReference(BigInteger id, long version, Class<? extends PersistableItem> itemClass) {
        this.id = id;
        this.version = version;
        this.itemClass = itemClass;
    }

    public BigInteger getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Class<? extends PersistableItem> getItemClass() {
        return itemClass;
    }
}
