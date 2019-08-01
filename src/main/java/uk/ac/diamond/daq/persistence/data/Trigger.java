package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.io.Serializable;

public abstract class Trigger extends PersistableItem implements Serializable {
    private Scan scan;

    @Persisted(key = true)
    @Searchable("name")
    @Listable("Name")
    private String name;

    Trigger(String name, Scan scan) {
        this.name = name;
        this.scan = scan;
    }

    public String getName() {
        return name;
    }

    public Scan getScan() {
        return scan;
    }

    public abstract void validate ();
}
