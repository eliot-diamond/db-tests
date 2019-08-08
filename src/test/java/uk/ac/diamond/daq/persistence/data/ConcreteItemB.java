package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.util.Objects;

public class ConcreteItemB extends AbstractItem {
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemB.class);

    @Persisted
    @Listable("Property 1")
    @Searchable("property1")
    private int property1;

    @Persisted
    @Listable("Property 3")
    @Searchable("property3")
    private double property3;

    @JsonCreator
    public ConcreteItemB(@JsonProperty("name") String name, @JsonProperty("property1") int property1,
                         @JsonProperty("property3") double property3) {
        super(name);

        this.property1 = property1;
        this.property3 = property3;
    }

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
