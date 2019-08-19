package uk.ac.diamond.daq.persistence.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ItemContainer extends ItemReference {
    @JsonProperty
    private String payload;

    @JsonIgnore
    private List<Class<?>> classes;

    @JsonProperty
    private String visitId;

    public ItemContainer(PersistableItem item, String payload, String visitId) {
        super(item);
        this.payload = payload;
        this.visitId = visitId;

        createClasses();
    }

    @JsonCreator
    private ItemContainer(@JsonProperty("id") long id, @JsonProperty("version") long version,
                          @JsonProperty("className") String className) throws ClassNotFoundException {
        super(id, version, className);

        createClasses();
    }

    public ItemContainer(ItemContainer itemContainer, long version, String visitId) {
        super(itemContainer);

        payload = itemContainer.payload;
        this.version = version;
        this.visitId = visitId;
    }

    private void createClasses() {
        classes = new ArrayList<>();
        Class<?> clazz = getItemClass();
        while (clazz != null && !PersistableItem.class.equals(clazz)) {
            classes.add(clazz);
            clazz = clazz.getSuperclass();
        }
    }

    @JsonProperty("classNames")
    private List<String> getClassNames() {
        List<String> classNames = new ArrayList<>();
        for (Class<?> clazz : classes) {
            classNames.add(clazz.getCanonicalName());
        }
        return classNames;
    }

    @JsonIgnore
    public String getJson() {
        return payload;
    }

    @JsonIgnore
    public String getVisitId() {
        return visitId;
    }
}
