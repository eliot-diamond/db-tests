package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Searchable;

import java.io.Serializable;
import java.util.Objects;

public abstract class PersistableItem implements Serializable {
    public static final long INVALID_ID = -1;

    @Searchable("id")
    private long id;

    @Listable(value = "Version", priority = 1000)
    private long version;

    protected PersistableItem() {
        this.id = INVALID_ID;
        this.version = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getVersion() {
        return version;
    }

    public void incrementVersion() {
        version++;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PersistableItem) {
            return ((PersistableItem) obj).id == id && ((PersistableItem) obj).version == version;
        }
        return super.equals(obj);
    }
}
