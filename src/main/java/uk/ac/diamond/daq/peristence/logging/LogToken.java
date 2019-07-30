package uk.ac.diamond.daq.peristence.logging;

import uk.ac.diamond.daq.peristence.annotation.Listable;
import uk.ac.diamond.daq.peristence.annotation.Searchable;
import uk.ac.diamond.daq.peristence.data.PersistableItem;

import java.util.Date;
import java.util.List;

public class LogToken extends PersistableItem {
    public static final String SEARCH_DATE_FIELD = "Log created Date";

    private String description;

    private List<Long> persistedItemIds;

    public LogToken(String description, List<Long> persistedItemIds) {
        super (new Date().getTime());
        this.description = description;
        this.persistedItemIds = persistedItemIds;
    }

    @Listable("Log Created")
    @Searchable(SEARCH_DATE_FIELD)
    public Date getDate() {
        return new Date(getId());
    }

    @Listable("Change Description")
    public String getDescription() {
        return description;
    }

    public List<Long> getPersistedItemIds() {
        return persistedItemIds;
    }
}
