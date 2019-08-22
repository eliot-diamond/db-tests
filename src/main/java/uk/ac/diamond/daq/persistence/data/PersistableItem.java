package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;

import java.util.Objects;
@NodeEntity
public class PersistableItem extends Persistable {
    public static final long INVALID_ID = -1;

    protected PersistableItem() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);

    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}
