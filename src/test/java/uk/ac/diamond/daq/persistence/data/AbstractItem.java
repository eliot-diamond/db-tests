package uk.ac.diamond.daq.persistence.data;

import java.util.Objects;

public abstract class AbstractItem extends PersistableItem {
    public static final String SEARCH_NAME_FIELD = "name";

    private String name;

    protected AbstractItem(String name) {
        super();
        this.name = name;
    }

    public AbstractItem(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void execute();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbstractItem abstractItem = (AbstractItem) o;
        return Objects.equals(name, abstractItem.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
