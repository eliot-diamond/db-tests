package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;

import java.io.Serializable;
import java.util.Objects;

public abstract class PersistableItem implements Serializable {
    public static final long INVALID_ID = -1;

    @Persisted(key = true)
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistableItem that = (PersistableItem) o;
        return version == that.version && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
