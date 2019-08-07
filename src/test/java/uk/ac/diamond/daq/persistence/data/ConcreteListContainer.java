package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.util.ArrayList;
import java.util.List;

public class ConcreteListContainer extends PersistableItem {
    private static final Logger log = LoggerFactory.getLogger(ConcreteListContainer.class);

    @Persisted(key = true)
    @Searchable("name")
    @Listable("Name")
    private String name;

    @Persisted
    private List<AbstractItemContainer> abstractItemContainers;

    @JsonCreator
    public ConcreteListContainer(@JsonProperty("name") String name) {
        this.name = name;

        abstractItemContainers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addTrigger(AbstractItemContainer abstractItemContainer) {
        abstractItemContainers.add(abstractItemContainer);
    }

    public List<AbstractItemContainer> getAbstractItemContainers() {
        return abstractItemContainers;
    }

    public void start() {
        log.info("Started plan {} (id: {}, version: {})", name, getId(), getVersion());

        for (AbstractItemContainer abstractItemContainer : abstractItemContainers) {
            abstractItemContainer.execute();
        }
    }
}
