package uk.ac.diamond.daq.persistence.data;

import java.util.Date;
import java.util.Set;

public class LogToken extends PersistableItem {
    private Date date;

    private String description;

    private Set<ItemReference> itemReferences;

    public LogToken(String description, Set<ItemReference> itemReferences) {
        this.date = new Date();
        this.description = description;
        this.itemReferences = itemReferences;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public Set<ItemReference> getItemReferences() {
        return itemReferences;
    }
}
