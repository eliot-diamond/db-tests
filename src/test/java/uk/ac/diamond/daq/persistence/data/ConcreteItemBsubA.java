package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NodeEntity
public class ConcreteItemBsubA extends ConcreteItemB {

    public static final String UNIQUE_FIELD = "prop4";
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemBsubA.class);

    private int property4;

    public ConcreteItemBsubA(String name, int property1,
                             double property3, int property4) {
        super(name, property1, property3);

        this.property4 = property4;
    }

    public ConcreteItemBsubA(){}

    public int getProperty4() {
        return property4;
    }

    @Override
    public void execute() {
        log.info("Executing ConcreteItemBsubA {} (id: {}, version: {}) property1: {}, property3: {}, property4: {}", getName(), getId(),
                getVersion(), getProperty1(), getProperty3(), getProperty4());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteItemBsubA that = (ConcreteItemBsubA) o;
        return (getProperty4() == that.getProperty4());
    }

}
