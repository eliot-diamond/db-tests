package uk.ac.diamond.daq.persistence.data;

import java.util.Date;
import java.util.List;

public class LogToken extends PersistableItem {
    private Date date;

    private String description;

    private List<ItemReference> itemReferences;

    public LogToken(String description, List<ItemReference> itemReferences) {
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

    public List<ItemReference> getItemReferences() {
        return itemReferences;
    }
}
