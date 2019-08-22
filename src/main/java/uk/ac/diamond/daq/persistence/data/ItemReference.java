package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.Objects;
@NodeEntity
public class ItemReference {
    @JsonProperty
    long version;
    @JsonProperty
    private long id;
    @JsonIgnore
    private Class<? extends Persistable> itemClass;

    @SuppressWarnings("unchecked")
    @JsonCreator
    protected ItemReference(@JsonProperty("id") long id, @JsonProperty("version") long version,
                            @JsonProperty("itemClass") String className) throws ClassNotFoundException {
        this.id = id;
        this.version = version;
        itemClass = (Class<? extends Persistable>) Class.forName(className);
    }

    public ItemReference(long id, long version, Class<? extends Persistable> itemClass) {
        this.id = id;
        this.version = version;
        this.itemClass = itemClass;
    }

    public ItemReference(ItemReference itemReference) {
        id = itemReference.id;
        version = itemReference.version;
        itemClass = itemReference.itemClass;
    }

    public ItemReference(Persistable item) {
        id = item.getId();
        version = item.getVersion();
        itemClass = item.getClass();
    }

    public long getId() {
        return id;
    }

    public void setId(long id){
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    @JsonProperty
    public Class<? extends Persistable> getItemClass() {
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
