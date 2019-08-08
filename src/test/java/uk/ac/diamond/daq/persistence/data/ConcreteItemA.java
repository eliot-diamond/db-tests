package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;

public class ConcreteItemA extends AbstractItem {
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemA.class);

    @Persisted
    private int property1;
    @Persisted
    private int property2;

    @JsonCreator
    public ConcreteItemA(@JsonProperty("name") String name, @JsonProperty("property1") int property1,
                         @JsonProperty("property2") int property2) {
        super(name);

        this.property1 = property1;
        this.property2 = property2;
    }

    @Listable(value = "Property 1", priority = 1)
    public int getProperty1() {
        return property1;
    }

    @Listable(value = "Property 2", priority = 2)
    public int getProperty2() {
        return property2;
    }

    @Override
    public void execute() {
        log.info("Executing ConcreteItemA {} (id: {}, version: {}) property1: {}, property3: {}", getName(), getId(),
                getVersion(), property1, property2);
    }
}
