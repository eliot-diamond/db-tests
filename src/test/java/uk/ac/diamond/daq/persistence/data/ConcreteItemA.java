package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NodeEntity
public class ConcreteItemA extends AbstractItem {

    public static final String CLASS_UNIQUE_FIELD = "uniqueProperty";
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemA.class);
    private int property1;
    private int property2;
    private String property3 = CLASS_UNIQUE_FIELD;

    public ConcreteItemA(String name, int property1,
                         int property2, String property3) {
        super(name);

        this.property1 = property1;
        this.property2 = property2;
        this.property3 = property3;
    }

    public ConcreteItemA(){}

    public int getProperty1() {
        return property1;
    }

    public int getProperty2() {
        return property2;
    }

    public String getProperty3() {
        return property3;
    }

    @Override
    public void execute() {
        log.info("Executing ConcreteItemA {} (id: {}, version: {}) property1: {}, property2: {}, {}: {}", getName(), getId(),
                getVersion(), property1, property2, CLASS_UNIQUE_FIELD, property3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteItemA that = (ConcreteItemA) o;
        return (that.getProperty1() == this.getProperty1() && that.getProperty2() == this.getProperty2() && that.getProperty3().equals(this.getProperty3()));
    }
}
