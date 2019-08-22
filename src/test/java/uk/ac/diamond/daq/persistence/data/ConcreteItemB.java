package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@NodeEntity
public class ConcreteItemB extends AbstractItem {
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemB.class);


    private int property1;
    private double property3;

    public ConcreteItemB(String name, int property1,
                         double property3) {
        super(name);

        this.property1 = property1;
        this.property3 = property3;
    }

    public ConcreteItemB(){}

    public double getProperty3() {
        return property3;
    }

    public int getProperty1() {
        return property1;
    }

    public void setProperty1(int property1) {
        this.property1 = property1;
    }

    @Override
    public void execute() {
        log.info("Executing ConcreteItemB {} (id: {}, version: {}) property1: {}, property3: {}", getName(), getId(),
                getVersion(), property1, property3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteItemB that = (ConcreteItemB) o;
        return property1 == that.property1 &&
                Double.compare(that.property3, property3) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), property1, property3);
    }
}
