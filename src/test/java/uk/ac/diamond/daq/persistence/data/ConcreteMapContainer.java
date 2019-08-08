package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.diamond.daq.persistence.annotation.Listable;
import uk.ac.diamond.daq.persistence.annotation.Persisted;
import uk.ac.diamond.daq.persistence.annotation.Searchable;

import java.util.HashMap;
import java.util.Map;

public class ConcreteMapContainer extends PersistableItem {
    private static final Logger log = LoggerFactory.getLogger(ConcreteMapContainer.class);

    @Persisted(key = true)
    @Listable("Name")
    @Searchable("name")
    private String name;

    @Persisted
    @JsonProperty
    private Map<String, AbstractItem> map;

    @JsonCreator
    public ConcreteMapContainer(@JsonProperty("name") String name) {
        this.name = name;

        map = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void addItem(String name, AbstractItem item) {
        map.put(name, item);
    }

    public void execute() {
        log.info("Executing {} (id: {}, version: {})", name, getId(), getVersion());

        map.forEach((key, item) -> {
            log.info("Executing: {}", key);
            item.execute();
        });
    }
}
