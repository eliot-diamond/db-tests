package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

public abstract class AbstractItemContainer extends PersistableItem {
    private AbstractItem abstractItem;

    @Persisted(key = true)
    @Searchable("name")
    @Listable("Name")
    private String name;

    AbstractItemContainer(String name, AbstractItem abstractItem) {
        this.name = name;
        this.abstractItem = abstractItem;
    }

    public String getName() {
        return name;
    }

    public AbstractItem getAbstractItem() {
        return abstractItem;
    }

    public abstract void execute();
}
