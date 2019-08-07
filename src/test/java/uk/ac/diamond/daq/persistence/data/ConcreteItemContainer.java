package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Persisted;

public class ConcreteItemContainer extends AbstractItemContainer {
    private static final Logger log = LoggerFactory.getLogger(ConcreteItemContainer.class);
    @Persisted
    private double property4;

    @JsonCreator
    public ConcreteItemContainer(@JsonProperty("name") String name, @JsonProperty("scan") AbstractItem abstractItem,
                                 @JsonProperty("property4") double property4) {
        super(name, abstractItem);
        this.property4 = property4;
    }

    @Override
    public void execute() {
        log.info("Trigger {} (id: {}, version: {}) property4: {}", getName(), getId(), getVersion(), property4);
        getAbstractItem().run();
    }

    public double getProperty4() {
        return property4;
    }
}
