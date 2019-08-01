package uk.ac.diamond.daq.persistence.logging;

import java.math.BigInteger;
import java.util.Objects;

public class ItemReference {
    private BigInteger id;

    private long version;

    public ItemReference(BigInteger id, long version) {
        this.id = id;
        this.version = version;
    }

    public BigInteger getId() {
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
            return ((ItemReference) obj).id.equals(id) && ((ItemReference) obj).version == version;
        }
        return super.equals(obj);
    }
}
