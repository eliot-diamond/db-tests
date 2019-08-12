package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;


public class ConcreteItemA extends AbstractItem {

    private static final Logger log = LoggerFactory.getLogger(ConcreteItemA.class);

    public static final String CLASS_UNIQUE_FIELD = "uniqueProperty";

    @Persisted
    private int property1;
    @Persisted
    private int property2;
    @Persisted
    @Searchable(CLASS_UNIQUE_FIELD)
    private String property3 = CLASS_UNIQUE_FIELD;

    @JsonCreator
    public ConcreteItemA(@JsonProperty("name") String name, @JsonProperty("property1") int property1,
                         @JsonProperty("property2") int property2, @JsonProperty("CLASS_UNIQUE_FIELD") String property3) {
        super(name);

        this.property1 = property1;
        this.property2 = property2;
        this.property3 = property3;
    }

    @Listable(value = "Property 1", priority = 1)
    public int getProperty1() {
        return property1;
    }

    @Listable(value = "Property 2", priority = 2)
    public int getProperty2() {
        return property2;
    }

    @Listable(value = CLASS_UNIQUE_FIELD, priority = 8)
    public String getProperty3() {
        return property3;
    }

    @Override
    public void execute() {
        log.info("Executing ConcreteItemA {} (id: {}, version: {}) property1: {}, property2: {}, {}: {}", getName(), getId(),
                getVersion(), property1, property2, CLASS_UNIQUE_FIELD, property3);
    }
}
