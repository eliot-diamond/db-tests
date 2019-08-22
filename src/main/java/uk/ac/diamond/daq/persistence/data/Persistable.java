package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

import java.io.Serializable;

public abstract class Persistable implements Serializable, Cloneable {

    protected long version;
    @Id
    @GeneratedValue
    protected Long id;

    public long getId(){
        return this.id;
    }

    public void setId(long id){
        this.id = id;
    }

    public Persistable() {
        this.version = 0;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean equals(Object o){
        if (this == o){return true;};
        if (o == null || getClass() != o.getClass()) return false;
        Persistable that = (Persistable) o;
        return (version == that.version && id == that.id);

    }
}
