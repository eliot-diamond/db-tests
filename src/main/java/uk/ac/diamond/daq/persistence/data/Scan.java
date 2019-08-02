package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.io.Serializable;
import java.util.Objects;

public abstract class Scan extends PersistableItem implements Serializable {
    public static final String SEARCH_NAME_FIELD = "name";

    @Persisted(key = true)
    @Searchable(SEARCH_NAME_FIELD)
    @Listable("Scan Name")
    private String name;

    Scan() {
        // default constructor for use by JSON
    }

    Scan(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void run();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Scan scan = (Scan) o;
        return Objects.equals(name, scan.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
