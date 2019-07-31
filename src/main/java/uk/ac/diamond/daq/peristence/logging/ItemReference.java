package uk.ac.diamond.daq.peristence.logging;

import java.util.Objects;

public class ItemReference {
    private long id;

    private long version;

    public ItemReference(long id, long version) {
        this.id = id;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemReference) {
            return ((ItemReference) obj).id == id && ((ItemReference) obj).version == version;
        }
        return super.equals(obj);
    }
}
