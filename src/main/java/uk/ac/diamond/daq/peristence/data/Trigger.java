package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Searchable;

public abstract class Trigger {
    private Scan scan;

    @Searchable("name")
    private String name;

    public abstract void validate ();
}
