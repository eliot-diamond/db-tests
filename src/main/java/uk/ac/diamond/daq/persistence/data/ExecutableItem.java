package uk.ac.diamond.daq.persistence.data;

public abstract class ExecutableItem extends PersistableItem {
    private String name;

    public ExecutableItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void start();
}
