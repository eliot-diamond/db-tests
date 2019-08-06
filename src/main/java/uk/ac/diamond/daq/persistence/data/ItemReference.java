package uk.ac.diamond.daq.persistence.data;

import java.math.BigInteger;

public class ItemReference {
    private BigInteger persistenceId;
    private long version;

    private Class<? extends PersistableItem> itemClass;

    public ItemReference(BigInteger persistenceId, long version, Class<? extends PersistableItem> itemClass) {
        this.persistenceId = persistenceId;
        this.itemClass = itemClass;
    }

    public BigInteger getPersistenceId() {
        return persistenceId;
    }

    public long getVersion() {
        return version;
    }

    public Class<? extends PersistableItem> getItemClass() {
        return itemClass;
    }
}
