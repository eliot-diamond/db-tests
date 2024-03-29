package uk.ac.diamond.daq.persistence.data;

import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

public abstract class AbstractItemContainer extends PersistableItem {
    public static final String CONTAINER_NAME = "name";
    @Persisted
    private AbstractItem abstractItem;
    @Persisted(key = true)
    @Searchable(CONTAINER_NAME)
    @Listable("Name")
    private String name;

    AbstractItemContainer(String name, AbstractItem abstractItem) {
        this.name = name;
        this.abstractItem = abstractItem;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public AbstractItem getAbstractItem() {
        return abstractItem;
    }

    public void setAbstractItem(AbstractItem theItem) {
        this.abstractItem = theItem;
    }

    public abstract void execute();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbstractItemContainer that = (AbstractItemContainer) o;
        return (that.name.equals(this.name)
                && that.getAbstractItem().getName().equals(this.getAbstractItem().getName())
                && that.getAbstractItem().getVersion() == this.getAbstractItem().getVersion());
    }
}
