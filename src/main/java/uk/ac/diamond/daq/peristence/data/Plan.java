package uk.ac.diamond.daq.peristence.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Persisted;
import uk.ac.diamond.daq.peristence.annotation.Searchable;

import java.util.List;

public class Plan extends PersistableItem {
    private static final Logger log = LoggerFactory.getLogger(Plan.class);

    @Persisted(key = true)
    @Searchable("name")
    @Listable("Plan Name")
    private String name;

    @Persisted
    List<Trigger> triggers;

    public String getName() {
        return name;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void start() {
        log.info("Started plan {} (id: {}, version: {})", name, getId(), getVersion());

        for (Trigger trigger : triggers) {
            trigger.validate();
        }
    }
}
