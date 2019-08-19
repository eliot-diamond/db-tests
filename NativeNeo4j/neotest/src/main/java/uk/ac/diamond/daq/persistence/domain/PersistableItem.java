package uk.ac.diamond.daq.persistence.domain;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@NodeEntity(label = "All_items")
public abstract class PersistableItem implements Serializable, Cloneable {
    public static long ID = 100;
    protected PersistableItem previousVersion;
    @Id
    @GeneratedValue
    private long id;
    @Property
    private long version;
    private String name;

    protected PersistableItem() {
        this.id = ++ID;
        this.version = 0;
    }

    protected PersistableItem(PersistableItem source) {
        this.id = source.id;
    }

    public void update() {
        this.setVersion(this.getVersion() + 1);
    }

    public String getName() {
        return this.name;
    }

    protected void setName(String newName) {
        this.increment();
        this.name = newName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PersistableItem that = (PersistableItem) o;
        return version == that.version && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    protected abstract void increment();

}
