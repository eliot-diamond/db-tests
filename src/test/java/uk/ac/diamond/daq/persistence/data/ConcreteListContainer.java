package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
@NodeEntity
public class ConcreteListContainer extends PersistableItem {
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteListContainer.class);


    private String name;

    private List<AbstractItemContainer> abstractItemContainers;


    public ConcreteListContainer(String name) {
        super();
        this.name = name;

        abstractItemContainers = new ArrayList<>();
    }

    public ConcreteListContainer(){}

    public String getName() {
        return name;
    }

    public void addTrigger(AbstractItemContainer abstractItemContainer) {
        abstractItemContainers.add(abstractItemContainer);
        abstractItemContainer.setHolder(this);
    }

    public List<AbstractItemContainer> getAbstractItemContainers() {
        return abstractItemContainers;
    }

    public void execute() {
        log.info("Started plan {} (id: {}, version: {})", name, getId(), getVersion());

        for (AbstractItemContainer abstractItemContainer : abstractItemContainers) {
            abstractItemContainer.execute();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteListContainer that = (ConcreteListContainer) o;
        return (that.getName().equals(this.getName()) && that.getAbstractItemContainers().equals(this.getAbstractItemContainers()));
    }

}
