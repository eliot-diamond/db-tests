package uk.ac.diamond.daq.persistence.service;

import java.math.BigInteger;
import java.util.Map;

public class SearchResultRow {
    private BigInteger persistenceId;
    private long version;

    private Map<SearchResultHeading, String> values;

    public SearchResultRow(BigInteger persistenceId, long version, Map<SearchResultHeading, String> values) {
        this.persistenceId = persistenceId;
        this.version = version;
        this.values = values;
    }

    public BigInteger getPersistenceId() {
        return persistenceId;
    }

    public long getVersion() {
        return version;
    }

    public Map<SearchResultHeading, String> getValues() {
        return values;
    }
}
