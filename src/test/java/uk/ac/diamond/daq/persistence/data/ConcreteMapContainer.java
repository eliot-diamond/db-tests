package uk.ac.diamond.daq.persistence.data;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@NodeEntity

public class ConcreteMapContainer extends PersistableItem {
    @Transient
    private static final Logger log = LoggerFactory.getLogger(ConcreteMapContainer.class);


    private String name;


    private Map<String, AbstractItem> map;


    public ConcreteMapContainer(String name) {
        super();
        this.name = name;

        map = new HashMap<>();
    }

    public ConcreteMapContainer(){}

    public AbstractItem getItem(String key) {
        return map.get(key);
    }

    public String getName() {
        return name;
    }

    public Map<String, AbstractItem> getMap(){
        return this.map;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConcreteMapContainer that = (ConcreteMapContainer) o;
        return (that.getName().equals(this.getName()) && that.getMap().equals(this.getMap()));
    }
}
