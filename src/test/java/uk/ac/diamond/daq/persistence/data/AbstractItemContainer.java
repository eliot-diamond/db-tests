package uk.ac.diamond.daq.persistence.data;

public abstract class AbstractItemContainer extends MapHolder {

    public static final String CONTAINER_NAME = "name";

    private String name;

    protected AbstractItemContainer(String name, AbstractItem abstractItem, PersistableItem holder) {
        super(name, abstractItem, holder);
        this.name = name;
    }

    AbstractItemContainer(){}

    public AbstractItemContainer(String name, AbstractItem abstractItem) {
        super (name, abstractItem);
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public abstract void execute();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbstractItemContainer that = (AbstractItemContainer) o;
        return (that.name.equals(this.name));
    }
}
