package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Persisted;
import uk.ac.diamond.daq.peristence.annotation.Searchable;

import java.io.Serializable;

public abstract class Scan extends PersistableItem implements Serializable {
    public static final String SEARCH_NAME_FIELD = "name";

    @Persisted(key = true)
    @Searchable(SEARCH_NAME_FIELD)
    @Listable("Scan Name")
    private String name;

    Scan(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void run();
}
