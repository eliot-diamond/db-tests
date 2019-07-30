package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Searchable;

public abstract class Scan extends PersistableItem {
    public static final String SEARCH_NAME_FIELD = "name";

    @Searchable(SEARCH_NAME_FIELD)
    @Listable("Scan Name")
    private String name;

    Scan(long id, String name) {
        super (id);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
