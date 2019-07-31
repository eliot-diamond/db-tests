package uk.ac.diamond.daq.peristence.service;

import java.util.Map;

public class SearchResultRow {
    private long persistenceId;
    private Map<SearchResultHeading, String> values;

    public SearchResultRow(long persistenceId, Map<SearchResultHeading, String> values) {
        this.persistenceId = persistenceId;
        this.values = values;
    }

    public long getPersistenceId() {
        return persistenceId;
    }

    public Map<SearchResultHeading, String> getValues() {
        return values;
    }
}
