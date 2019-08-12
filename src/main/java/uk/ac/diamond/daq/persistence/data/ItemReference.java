package uk.ac.diamond.daq.persistence.data;

import java.math.BigInteger;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ItemReference) {
            ItemReference that = (ItemReference) o;
            return Objects.equals(id, that.id) && version == that.version;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
