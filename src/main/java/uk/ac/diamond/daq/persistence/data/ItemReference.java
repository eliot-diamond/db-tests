package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ItemReference {
    @JsonProperty
    private long id;

    @JsonProperty
    private long version;

    @JsonIgnore
    private Class<? extends PersistableItem> itemClass;

    @SuppressWarnings("unchecked")
    protected ItemReference(long id, long version, String className) throws ClassNotFoundException {
        this.id = id;
        this.version = version;
        itemClass = (Class<? extends PersistableItem>) Class.forName(className);
    }

    public ItemReference(long id, long version, Class<? extends PersistableItem> itemClass) {
        this.id = id;
        this.version = version;
        this.itemClass = itemClass;
    }

    public long getId() {
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
            return id == that.id && version == that.version;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
