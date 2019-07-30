package uk.ac.diamond.daq.peristence.data;

import uk.ac.diamond.daq.peristence.annotation.Searchable;

public abstract class PersistableItem {
    @Searchable("id")
    private long id;
    private long version;

    protected PersistableItem(long id) {
        this.id = id;
        this.version = 0;
    }

    public long getId() {
        return id;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getVersion() {
        return version;
    }
/*
    public PersistableItem clone () {
        PersistableItem item = cloneInternal();
        version++;
        return item;
    }

    protected abstract PersistableItem cloneInternal ();

 */
}
