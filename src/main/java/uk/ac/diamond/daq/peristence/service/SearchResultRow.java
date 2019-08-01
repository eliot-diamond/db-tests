package uk.ac.diamond.daq.peristence.service;

import java.util.Map;

public class SearchResultRow {
    private long persistenceId;
    private long version;

    private Map<SearchResultHeading, String> values;

    public SearchResultRow(long persistenceId, long version, Map<SearchResultHeading, String> values) {
        this.persistenceId = persistenceId;
        this.version = version;
        this.values = values;
    }

    public long getPersistenceId() {
        return persistenceId;
    }

    public long getVersion() {
        return version;
    }

    public Map<SearchResultHeading, String> getValues() {
        return values;
    }
}
