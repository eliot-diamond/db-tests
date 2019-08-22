package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcreteItemContainer extends AbstractItemContainer {
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemContainer.class);
    private double property4;

    public ConcreteItemContainer(String name, AbstractItem abstractItem,
                                 double property4) {
        super(name, abstractItem);
        this.property4 = property4;
    }

    public ConcreteItemContainer() {

    }

    @Override
    public void execute() {
        log.info("Trigger {} (id: {}, version: {}) property4: {}", getName(), getId(), getVersion(), property4);
    }

    public double getProperty4() {
        return property4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteItemContainer that = (ConcreteItemContainer) o;
        return (that.getProperty4() == this.getProperty4());
    }
}
