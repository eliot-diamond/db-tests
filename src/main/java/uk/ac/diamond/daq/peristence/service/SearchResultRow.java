package uk.ac.diamond.daq.peristence.service;

import java.util.Map;

public class SearchResultRow<T> {
    private T item;
    private Map<SearchResultHeading, String> values;

    public SearchResultRow(T item, Map<SearchResultHeading, String> values) {
        this.item = item;
        this.values = values;
    }

    public T getItem() {
        return item;
    }

    public Map<SearchResultHeading, String> getValues() {
        return values;
    }
}
