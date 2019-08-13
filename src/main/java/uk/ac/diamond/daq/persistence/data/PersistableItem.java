package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

public abstract class PersistableItem implements Serializable {
    public static final BigInteger INVALID_ID = new BigInteger("-1");

    private static final int HEX_RADIX = 16;

    @Searchable("id")
    private BigInteger id;

    @Listable(value = "Version", priority = 1000)
    private long version;

    protected PersistableItem() {
        this.id = INVALID_ID;
        this.version = 0;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public void setId(String hexString) {
        this.id = new BigInteger(hexString, HEX_RADIX);
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void incrementVersion() {
        version++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistableItem that = (PersistableItem) o;
        return version == that.version &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
