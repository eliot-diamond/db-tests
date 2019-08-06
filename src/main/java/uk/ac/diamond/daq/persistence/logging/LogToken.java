package uk.ac.diamond.daq.persistence.logging;

import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.PersistableItem;

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
