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

public class Plan extends PersistableItem {
    private static final Logger log = LoggerFactory.getLogger(Plan.class);

    @Persisted(key = true)
    @Searchable("name")
    @Listable("Plan Name")
    private String name;

    @Persisted
    private List<Trigger> triggers;

    @JsonCreator
    public Plan(@JsonProperty("name") String name) {
        this.name = name;

        triggers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addTrigger(Trigger trigger) {
        triggers.add(trigger);
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
