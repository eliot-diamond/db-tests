package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

@RelationshipEntity
public class MapHolder extends Persistable {
    @StartNode
    private Persistable holder;
    private Object key;
    @EndNode
    private Persistable value;

    public MapHolder(Object key, Persistable value, Persistable holder) {
        this.value = value;
        this.key = key;
        this.holder = holder;
    }

    public MapHolder(Object key, Persistable value) {
        this.key = key;
        this.value = value;
    }

    public MapHolder() {

    }

    public void setHolder(Persistable holder) {
        this.holder = holder;
    }

    public Persistable getItem() {
        return this.value;
    }

    public Persistable getHolder() {
        return holder;
    }

    public Object getKey() {
        return key;
    }

    public Persistable getValue(){
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MapHolder that = (MapHolder) o;
        return (that.getHolder().equals(this.getHolder()) && that.getValue().equals(this.getValue()) && that.getKey().equals(this.getKey()));
    }
}
